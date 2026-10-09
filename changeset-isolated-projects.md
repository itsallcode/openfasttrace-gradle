# Changeset: Isolated Projects compatibility

- **Date:** 2026-10-09
- **Status:** planned
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

Note: the root project's *reads* (`rootProject.getAllprojects()`, used to aggregate input
directories, tag path configs and imported requirements during task realization) are legal under
Isolated Projects — only the *mutation* via `allprojects { … }` is the violation.

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

## Decision

**Option A — convention pattern** (user-approved, matches Gradle's Isolated Projects
recommendation): every project that configures `requirementTracing` applies the plugin to itself
in its own `build.gradle`. No settings plugin, no lifecycle-callback magic.

- Gradle 9.x only — do not re-enable the Gradle 8.14.5 test config.
- Documentation updated as part of this change.

## Changes

### 1. `src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`

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
- Verify: `./gradlew clean test` → all tests pass (21 before fix, 2 fail; 22 after adding the
  new test).

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

## Verification

1. `./gradlew clean test` → 21/21 pass (currently 2 fail before the fix).
2. Manual:

   ```sh
   cd example-projects/multi-project
   ./gradlew traceRequirements --isolated-projects -Dorg.gradle.isolated-projects.diagnostics=true
   ```

   → violation-free build (zero Isolated Projects problems).
3. `--warning-mode fail` holds — no new deprecations.

## Notes / risks

- **Status:** implementation in progress. Per-project DSL + root-only tasks are done; the
  files-channel (`oftExportConfig` tasks + root consumption via task-path `dependsOn` and
  `@InputFile` wiring) is the remaining core work. The spike invalidated the earlier
  "aggregation reads are legal" assumption — see "Findings from the implementation spike".
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
