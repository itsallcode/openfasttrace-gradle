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

## Decision

**Option A — convention pattern** (user-approved, matches Gradle's Isolated Projects
recommendation): every project that configures `requirementTracing` applies the plugin to itself
in its own `build.gradle`. No settings plugin, no lifecycle-callback magic.

- Gradle 9.x only — do not re-enable the Gradle 8.14.5 test config.
- Documentation updated as part of this change.

## Changes

### 1. `src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`

- `apply(project)`: create the `requirementTracing` + `tags` DSL on the receiving project only
  (drop `rootProject.allprojects(...)`); register `collectRequirements` / `traceRequirements`
  tasks only when `project == project.getRootProject()`.
- Keep `createCollectTask` / `configureTracingTask` / `getConfig` and the
  `rootProject.getAllprojects()` aggregation as-is (these run at task realization, after all
  projects are configured).
- Prefer `getExtensions().findByType(...)` over `getByType(...)` when aggregating subprojects so
  projects that do not apply the plugin are tolerated (no forced plugin application).

### 2. `example-projects/multi-project`

- `sub1/build.gradle`: add

  ```groovy
  plugins {
      id 'org.itsallcode.openfasttrace'
  }
  ```

  above the existing `requirementTracing { tags { … } }` block.
- `sub2/build.gradle`: **intentionally does not apply the plugin.** This exercises the tolerant
  aggregation from change 1: the root project must still build and trace successfully when a
  subproject is not part of the tracing setup.

### 3. Tests

- Keep `--isolated-projects` in `PluginTestFixture.createGradleRunner()`.
- `GradleTestConfig` stays `CURRENT_VERSION("9.8.1")` only.
- Add integration test: multi-project build under `--isolated-projects` where **a subproject does
  not use the plugin** (e.g. `sub2` in `example-projects/multi-project`). Assert
  `traceRequirements` succeeds (`SUCCESS` / `FROM_CACHE` / `UP_TO_DATE`) and the report is
  generated, proving root aggregation tolerates subprojects without the plugin.
- Verify: `./gradlew clean test` → all tests pass (currently 20/20 after fix, 2 fail before).

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

- Phase 0 spike: before the main refactor, confirm that aggregating subproject extension data
  during task realization is Isolated Projects-legal. If a new `getExtensions` violation appears
  at task realization, fall back to a files-channel design (per-subproject serialized
  requirement-config files consumed by the root `collectRequirements` task as `@InputFiles`).
- User-facing change: subprojects that configure the DSL must now apply the plugin themselves.
- Upstream `main` still contains the `allprojects(...)` pattern; no upstream fix exists yet.

## Relevant files

- `src/main/java/org/itsallcode/openfasttrace/gradle/OpenFastTracePlugin.java`
- `example-projects/multi-project/{build.gradle, settings.gradle, sub1/build.gradle, sub2/build.gradle}`
- `src/test/java/org/itsallcode/openfasttrace/gradle/PluginTestFixture.java`
- `src/test/java/org/itsallcode/openfasttrace/gradle/GradleTestConfig.java`
- `README.md`
- `CHANGELOG.md`
