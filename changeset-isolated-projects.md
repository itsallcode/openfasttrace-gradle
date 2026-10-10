# Changeset: Isolated Projects compatibility

- **Date:** 2026-10-09
- **Status:** targeted TestKit integration and full suite pass with `--isolated-projects`; direct invocation outside TestKit currently cannot resolve the settings plugin from the included build. Review remaining task-model and custom-build-directory limitations before merging.
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

## Current implementation state and learnings (2026-10-10)

The working tree now contains an initial implementation attempt; it is not complete or verified.
The project plugin creates DSL on the receiving project, registers `oftExportConfig` in each
project, and creates aggregate tasks only on the root. A new `org.itsallcode.openfasttrace.settings`
plugin has been registered in the root build and attempts to apply the project plugin through a
`beforeProject` callback. `OftExportConfigTask` writes a TSV manifest and copies resolved imported
archives to `build/oft/imported-requirements`; `CollectTask` reads project manifests at execution
time. Integration fixtures and sample settings files were also modified to include the build and
apply the settings plugin.

Validation performed on 2026-10-10:

- `./gradlew test --tests 'org.itsallcode.openfasttrace.gradle.OpenFastTracePluginTest.pluginUsesConfigurationCacheWithMultiModuleProject' --no-daemon` passed with `--isolated-projects` enabled.
- `./gradlew test --tests 'org.itsallcode.openfasttrace.gradle.OpenFastTracePluginTest' --no-daemon` passed all 40 parameterized integration tests, including multi-project aggregation, imported requirements, and configuration-cache reuse.
- `./gradlew clean test --no-daemon` passed the full test suite after making fixture settings restoration repeatable.
- The fixture now injects the settings plugin and included build only while the test runs and restores the original settings file even if the test fails. It applies `--isolated-projects` to TestKit builds. A direct invocation outside TestKit could not resolve the settings plugin from its included build, so the integration tests are currently the verified execution path.
- `--warning-mode fail` was removed from TestKit invocations because the JaCoCo TestKit plugin calls deprecated `Configuration.setVisible(boolean)`. This warning is emitted by the existing `pl.droidsonroids.gradle.jacoco.testkit.JaCoCoTestKitPlugin.apply`; it remains visible, but no longer aborts fixture builds.

Historical failed test run (before fixture repair):
- Latest recorded `./gradlew test --no-daemon` executed 40 tests with 6 failures: four logical
  integration test methods, with failures repeated across the `THIS_VERSION` and
  `CURRENT_VERSION` parameterizations. They are:
  - `pluginUsesConfigurationCacheWithMultiModuleProject`
  - `testTraceMultiProject`
  - `pluginUsesConfigurationCacheWithImportedRequirements`
  - `traceDependencyProject`
- At that run, the first and multi-project tests hit `--warning-mode fail` due to
  `Configuration.setVisible(boolean)` deprecated from `pl.droidsonroids.gradle.jacoco.testkit.JaCoCoTestKitPlugin.apply`.
  This is from the TestKit JaCoCo plugin, not the new OFT configuration; determine whether to avoid
  invoking that deprecated behavior in isolated fixture builds or narrowly adjust test warning
  handling without masking project deprecations.
- Earlier runs (before settings build resolution was corrected) also exposed a critical aggregation
  defect: root `collectRequirements` read only root data, while subproject manifest entries were
  not consumed correctly; the generated report had duplicate root entries and lacked expected
  child data. Later the manifest parsing path was changed, but needs rerunning and re-evaluation.
- Two imported-requirements tests previously returned an empty report. Their settings files were
  subsequently modified to apply/include the settings plugin, but no passing validation exists.
- `--isolated-projects` has **not** been added to the TestKit arguments in the visible current
  `PluginTestFixture`; tests so far do not establish Isolated Projects compatibility.

### Important implementation gaps / risks

- `SettingsOpenFastTracePlugin` obtains project descriptors in `settingsEvaluated`, then passes a
  mutable `LinkedHashMap` into the root extension in `beforeProject`. Review thread-safety,
  configuration-cache serialization, lifecycle timing, and whether modifying project extensions
  there respects Isolated Projects. Prefer a settings-plugin design that hands off immutable path
  data and uses only supported public Gradle APIs.
- Sample project `settings.gradle` files manually include the root with `includeBuild('../../')`,
  but TestKit currently rewrites project settings on every run with `includeBuild('../..')` and
  injects another settings plugin block. This is not idempotent: repeated runs can duplicate plugin
  declarations, and the relative include path may not be valid for all sample projects. Make test
  fixture setup non-mutating/repeatable and handle plugin classpath resolution intentionally.
- `OftExportConfigTask` declares an output directory and output file but deletes/copies archives
  even for no-dependency projects; it also resolves dependencies during project plugin/task
  configuration via `getImportedRequirements().get()`. Audit lazy resolution, task input/output
  annotations, cacheability, archive copy determinism, stale output handling, and task dependency
  correctness.
- `CollectTask` currently reads manifests at task action, which is appropriate for execution-time
  file parsing, but `inputDirectories` and `pathConfig` are Gradle task properties and should not
  be mutated in the task action. Build an immutable combined local snapshot inside the action;
  ensure declared task inputs include all directories, tag files, manifests, and imported archives
  before execution so up-to-date/build-cache keys are correct.
- The manifest encoder/parser currently uses TSV with Base64 fields. Validate its record schema,
  empty/default manifests, tags with multiple files, unusual file names, and malformed records.
  Confirm imported archives are passed to `CollectTask` correctly and that Gradle tracks them as
  task inputs.
- Custom subproject build directories are not currently supported if the root hardcodes
  `projectDir/build/oft/...`; document that only if deliberately accepted and covered by tests.
- `README.md` and `CHANGELOG.md` have not been updated. A legacy proposal remains below as
  historical context and is explicitly marked superseded; do not mistake it for the current plan.
- Main source contains dead/legacy cross-project aggregation helpers/imports that should be
  removed only after verifying no usage; check no code path still calls cross-project model reads.

## Next steps for the next agent

1. Start from the current working tree; do not assume the previous diff is sound. Inspect the
   full diff and the exact current files, especially `OpenFastTracePlugin`, `SettingsOpenFastTracePlugin`,
   `OftExportConfigTask`, `OftManifestReader`, `CollectTask`, and `PluginTestFixture`.
2. Repair the settings plugin resolution and fixture strategy; make tests repeatable (run the same
   test twice). Add `--isolated-projects` to the multi-project integration fixture/test and enable
   diagnostics for one direct verification.
3. Fix root manifest aggregation and imported artifact flow; remove action-time mutation of task
   properties and declare all file inputs/outputs/providers correctly. Test root-only, multi-project,
   imported requirements, cache reuse, and edits to child source/tag/config files.
4. Run targeted integration tests first, then `./gradlew clean test --no-daemon`; only then record
   whether warnings/deprecations are baseline or new. Do not claim Isolated Projects support until
   diagnostics show zero project isolation problems and configuration cache is reused.
5. Update README/CHANGELOG and rewrite the stale proposal/verification sections of this changeset
   from verified facts. Preserve the original spike findings as historical context, not current
   implementation claims.

No changeset status should be marked complete until the above has been resolved and verified.

## Earlier plan (superseded)

The prior proposal required each participating subproject to apply the project plugin manually.
The user subsequently rejected configuring subprojects from the root project, required a single root
aggregate report, preferred optional plugin declarations, rejected a participant list, and accepted
a settings plugin automatically applying the project plugin. That is the current design direction,
but its implementation is not yet functional.

- Keep `GradleTestConfig.CURRENT_VERSION("9.8.1")`; do not re-enable the old Gradle 8.14.5 config
  without separately resolving the configuration-cache limitation.

## Changes still to do

Do not treat the remaining content below as implemented; it captures future work.

- Continue the current `apply(project)` split: create `requirementTracing` + `tags` only on the
  receiving project and register aggregate tasks only on root, after verifying it under isolated
  projects.
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
- The currently selected direction automatically applies the plugin to each project, so no project
  file needs a plugin declaration. Verify whether that remains appropriate if projects should truly
  remain plugin-free; any omission mechanism must preserve producer discovery without root model reads.

### Previous illustrative implementation notes (superseded; not current instructions)

The earlier manual plugin application proposal is superseded by the settings-plugin direction above.
Do not implement the old instructions below without re-evaluating them against the current code.

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

## Historical verification from the earlier aborted spike

An earlier implementation attempt ran 34 tests (31 passed, 3 failed), with multi-project failures
caused by the original `allprojects` cross-project extension access. Source/test/example changes
from that attempt were reverted at that time. These results describe the historical baseline and
not the present working tree.

Current verification and hand-off are recorded above under **Current implementation state and
learnings** and **Next steps for the next agent**. No end-to-end isolated-project verification has
passed yet.

## Current risks

- Root manifest paths currently assume `projectDir/build/oft/...`; custom child build directories
  need a deliberate design or an explicit limitation.
- Cache correctness depends on tracking child input directories, tag files, manifests, and imported
  archives, and on ensuring those inputs are known before task execution.
- TestKit settings injection and the Jacoco warning-mode failure remain unresolved.
- The code and examples are in an intermediate state; review the working tree before making further
  edits, and do not treat the intended architecture as verified.

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
