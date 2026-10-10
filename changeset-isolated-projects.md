# Changeset: Isolated Projects compatibility

- **Date:** 2026-10-09
- **Status:** implementation in progress — settings-plugin/file-channel design selected
- **Scope:** `OpenFastTracePlugin`, `example-projects/multi-project`, `README.md`, `CHANGELOG.md`

## Problem

Integration tests fail after enabling `--isolated-projects` in
`PluginTestFixture.createGradleRunner()` (`src/test/java/org/itsallcode/openfasttrace/gradle/PluginTestFixture.java`).

Two tests fail — `pluginUsesConfigurationCacheWithMultiModuleProject` and
`testTraceMultiProject` — both running against `example-projects/multi-project`, the only
example project with subprojects. Failure occurs during configuration, before any task runs:

```
Project ':' cannot access 'Project.extensions' functionality on subprojects via 'allprojects'
```

### Root cause

`OpenFastTracePlugin.apply()` (`src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`,
line 47) executes:

```java
rootProject.allprojects(OpenFastTracePlugin::createConfigDsl);
```

When the plugin is applied to the root project, it reaches into every subproject and creates the
`requirementTracing` and `tags` extensions there. Under Gradle Isolated Projects (Gradle 9.8.1),
accessing another project's **mutable** state (`extensions`) during configuration is a hard
constraint violation that fails the build.

The 18 single-project tests pass because there are no subprojects to touch.

Important: cross-project extension reads are also illegal under Isolated Projects, including reads made during task realization. Root aggregation therefore must use a file channel and cannot read any subproject model objects.

## Findings from the implementation spike (2026-10-09)

### Spike result: cross-project extension reads are violations even at task realization

The first refactor attempt (per-project DSL + root-only task registration, with
`getAllprojects()` aggregation still reading subproject extensions at task realization)
**failed**: the stack trace showed a new violation,

```
Project ':' cannot access 'Project.extensions' functionality on subprojects via 'allprojects'
    at org.itsallcode.openfasttrace.gradle.OpenFastTracePlugin.findConfig(OpenFastTracePlugin.java:223)
    at org.itsallcode.openfasttrace.gradle.OpenFastTracePlugin.getImportedRequirements(OpenFastTracePlugin.java:171)
    at org.itsallcode.openfasttrace.gradle.OpenFastTracePlugin.configureTracingTask(OpenFastTracePlugin.java:118)
```

So under Isolated Projects **any** cross-project model access is prohibited — including
`findByType(...)`, even when the read happens during task realization (inside
`tasks.register { ... }` / task configuration actions). The root project simply must not touch
subproject extensions at all. This rules out the "aggregation via `getAllprojects()`" design and
makes the **files-channel design mandatory**.

### Verified Isolated Projects-legal mechanisms (probes in `/tmp/ip-probe`, Gradle 9.8.1)

| Mechanism | Verdict |
| --- | --- |
| `dependsOn(':sub2:export')` (cross-project task dependency by task path) | ✅ legal, orders subproject task before root task |
| Project dependency (`project(':sub2')`) on a subproject **without** the plugin | ✅ legal — resolves, no violation (sub2 needs `base` plugin to expose variants; empty subproject → "No variants exist" is a normal Gradle error, not IP) |
| Root consuming a subproject output file declared from immutable paths (`projectDir`-relative) | ✅ legal (no IP violation; earlier `file()`-in-closure failures were Configuration Cache script-scope issues in the probe, not IP) |
| Any root → subproject `getExtensions()` / model read (even `findByType`, even at task realization) | ❌ IP violation |

Notes from the probes:
- A task action calling `file(...)` / `configurations` etc. from a Groovy closure is a
  **Configuration Cache** problem ("references a Gradle script object from a Groovy closure at
  execution time"), unrelated to Isolated Projects. Real tasks must be typed task classes
  (`CollectTask`/`TraceTask`) with properties declared at realization time.
- `buildDir` / `layout` are **mutable** cross-project state. The files-channel must derive the
  subproject config-JSON path from **immutable** data only: `projectDir` + a fixed relative path
  (`build/oft/...`), or the export task's declared output path via convention. Custom
  `buildDir` in subprojects will not be supported for cross-project aggregation.

## Spike findings and blocker (2026-10-09)

The implementation was stopped before an end-to-end fix was completed. The earlier decision
below to use per-project plugin application was not sufficient by itself: the root plugin still
needs a way to discover which subprojects opted into tracing, without reading mutable cross-project
state.

- The attempted partial refactor (per-project DSL, root-only tasks, plus `findByType` to aggregate
  child configuration) fails on Gradle 9.8.1 with `Project ':' cannot access 'Project.extensions'
  functionality on subprojects via 'allprojects'`. A configuration-cache/task-realization boundary
  does not make that access Isolated Projects-safe.
- A cross-project dependency declared as a task path is legal when the task exists, but it fails
  task graph construction if the named task is absent (`Task with name 'missing' not found in
  project ':sub'`). Thus blindly depending on every subproject's export task cannot provide the
  stated support for plugin-less subprojects.
- Root cannot determine whether a subproject applied the plugin by inspecting that project's
  extensions or task container. The user rejected an explicit participant list, requires one root
  aggregate report, and accepts a settings plugin that automatically applies the project plugin to
  each project. Per-project import configuration remains local.
- Selected architecture: a settings plugin uses the Gradle settings lifecycle to apply the project
  plugin to each project. Each project registers an export task writing a manifest. Root tasks use
  explicit task-path dependencies and immutable project-directory-derived manifest paths, then
  parse manifests at execution time. They do not inspect subproject extensions/tasks.
- A `beforeProject` callback probe applying a core plugin succeeded with Gradle 9.8.1 and both
  `--isolated-projects` and `--configuration-cache`. This does not prove custom plugin classloading
  or full manifest aggregation; those remain to be implemented and tested.
- Automatic plugin application means all projects receive the plugin, but project build scripts
  do not need a plugin block. Empty/default project configurations should export empty manifests.

Implementation may now proceed under this design. Update these findings with implementation and
verification results.

## Decision (superseded / not implemented)

The earlier **Option A — convention pattern** proposal stated that every project configuring
`requirementTracing` would apply the plugin itself, with no settings plugin or lifecycle-callback
magic. That proposal is incomplete until the root can aggregate the participating subprojects
without cross-project model reads. Do not treat this as an implementation decision.

- Gradle 9.x only — do not re-enable the Gradle 8.14.5 test config.
- Documentation updates were proposed but not made.

## Changes

### Proposed changes (not implemented)

#### 1. `src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`

- `apply(project)`: create the `requirementTracing` + `tags` DSL on the receiving project only
  (drop `rootProject.allprojects(...)`); register root tasks only when
  `project == project.getRootProject()`.
- **Files-channel aggregation (mandatory per spike):**
  - Each project that applies the plugin registers an `oftExportConfig` task (group `trace`)
    that serializes its own `requirementTracing` snapshot (input directories, resolved tag-path
    file lists, imported-requirement coordinates) to a config file under the project's
    `build/oft/` directory.
  - Root `collectRequirements` / `traceRequirements`:
    - `dependsOn` each subproject's `:subX:oftExportConfig` **by task path** (IP-legal).
    - Declare each config file as an `@InputFile` via paths computed from **immutable**
      `projectDir` (default build dir convention; custom `buildDir` in subprojects is not
      supported for aggregation).
    - Read the config files **at execution time** to build the import inputs (input dirs, path
      configs, imported requirements) — no cross-project model access.
- Tolerance: subprojects without the plugin are simply absent from the dependency graph (no
  `dependsOn`, no config file). No `findByType` / `getByType` on other projects is needed.
- Root's own config is still read directly (same project — no violation).

### 2. `example-projects/multi-project`

- `sub1/build.gradle`: add

  ```groovy
  plugins {
      id 'org.itsallcode.openfasttrace'
  }
  ```

  above the existing `requirementTracing { tags { … } }` block.
- `sub2/build.gradle`: **intentionally does not apply the plugin.** This exercises the tolerance
  case: the root project must still build and trace successfully when a subproject is not part of
  the tracing setup.

### 3. Tests

- Keep `--isolated-projects` in `PluginTestFixture.createGradleRunner()`.
- `GradleTestConfig` stays `CURRENT_VERSION("9.8.1")` only.
- Add integration test (in progress): multi-project build under `--isolated-projects` where **a
  subproject does not use the plugin** (`example-projects/multi-project-without-plugin`: root +
  `sub1` apply the plugin, `sub2` is plugin-less). Assert `traceRequirements` succeeds
  (`SUCCESS` / `FROM_CACHE` / `UP_TO_DATE`) and the report is generated, proving tolerance.
- Verify: `./gradlew clean test` → all tests pass (target counts still to be confirmed).

### 4. `README.md`

- "Usage" section: note the plugin applies to the project that declares it; in multi-project
  builds, each project that configures `requirementTracing` must declare the plugin itself.
- Add a short "Multi-project builds" subsection referencing `example-projects/multi-project`:
  the root project applies the plugin (owns `traceRequirements` / `collectRequirements`), each
  subproject that configures tags/imports applies the plugin too.
- Update the "Configuring the Short Tag Importer" section so the `multi-project/sub1` reference
  shows the plugin block in `sub1/build.gradle`.

### 5. `CHANGELOG.md`

- Add an `[Unreleased]` entry describing Isolated Projects compatibility (per-project plugin
  application; subprojects must apply the plugin themselves).

## Verification during the aborted implementation

`./gradlew test --no-daemon` ran 34 tests: 31 passed and 3 failed. The failing tests were:

- `pluginUsesConfigurationCacheWithMultiModuleProject`
- `testTraceMultiProject`
- `testTraceMultiProjectWithSubprojectWithoutPlugin`

All three fail while creating `:traceRequirements` with the Isolated Projects violation
`Project ':' cannot access 'Project.extensions' functionality on subprojects via 'allprojects'`.
The partial implementation did not yet have the files-channel design and must not be considered a
working fix. Source/test/example changes from that attempt were reverted; only this changeset now
records the findings. The test run also showed the suite currently executes 34 tests, so the earlier expected totals
(21/22) were outdated. The manual isolated-projects verification and warning-mode checks have not
been completed successfully.

## Remaining verification (not performed)

1. `./gradlew clean test` with the completed implementation.
2. `traceRequirements --isolated-projects -Dorg.gradle.isolated-projects.diagnostics=true` in the
   multi-project example, with zero Isolated Projects problems.
3. Verify `--warning-mode fail` introduces no new deprecations.

## Notes / risks

- **Status:** blocked. No implementation is currently present; the files-channel design and
  producer-discovery problem remain unresolved.
- **User-facing change:** subprojects that configure the DSL must now apply the plugin
  themselves.
- **Custom `buildDir` in subprojects:** not supported for cross-project aggregation (config
  files are read from `projectDir/build/oft/...` by convention).
- **Up-to-date tracking:** subproject source files referenced via the exported config JSON must
  be tracked as inputs of the consuming root task (declare the referenced directories/files in
  `CollectTask`) so the `FROM_CACHE` / `UP_TO_DATE` test assertions keep holding.
- Upstream `main` still contains the `allprojects(...)` pattern; no upstream fix exists yet.

## Relevant files

- `src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`
- `src/main/java/org/itsallcode/openfasttrace/gradle/task/CollectTask.java`
- `src/main/java/org/itsallcode/openfasttrace/gradle/task/TraceTask.java`
- (new) `src/main/java/org/itsallcode/openfasttrace/gradle/task/OftExportConfigTask.java` (or equivalent)
- `example-projects/multi-project/{build.gradle, settings.gradle, sub1/build.gradle, sub2/build.gradle}`
- `example-projects/multi-project-without-plugin/...` (new example project for the tolerance test)
- `src/test/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePluginTest.java`
- `src/test/java/org/itsallcode/openfasttrace/gradle/PluginTestFixture.java`
- `src/test/java/org/itsallcode/openfasttrace/gradle/GradleTestConfig.java`
- `README.md`
- `CHANGELOG.md`
