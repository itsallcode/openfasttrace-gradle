# Plan: Simplify plugin loading by consuming the new OFT plugin API

Status: planned
Upstream issue: [itsallcode/openfasttrace#610 — Allow adding plugins via configuration](https://github.com/itsallcode/openfasttrace/issues/610)
Related: [openfasttrace-gradle#77 — Add support for OpenFastTrace plugin dependencies](https://github.com/itsallcode/openfasttrace-gradle/issues/77)

## Goal

Delete the Gradle plugin's custom class-loader machinery and let OpenFastTrace (OFT)
load plugin JARs directly, once OFT exposes a public API for passing plugin JAR paths
(issue #610). This assumes the upstream feature is available in an upcoming OFT release.

## Why this is possible

The current complexity exists only because OFT's plugin loading is not programmable:

- `ServiceLoaderFactory` is package-private and hardcodes `$HOME/.oft/plugins`.
- `InitializingServiceLoader.load(Class<T>, C)` is the only public entry point and
  accepts no plugin locations.
- `ClassPathServiceLoader.filterOtherClassLoader` rejects any service whose
  `getClass().getClassLoader()` differs from the origin's class loader.

Because the Gradle plugin cannot tell OFT about extra plugin JARs, it hijacks the
thread context class loader (TCCL) with a child-first loader. Due to the class-loader
identity filter above, it must additionally locate and copy OFT's *built-in* provider
JARs (importer/exporter/reporter factories) into that same child loader — otherwise
built-in reporting and importing silently disappear as soon as any plugin is configured.

OFT already implements this correctly for `$HOME/.oft/plugins` via `ServiceOrigin` +
its own `ChildFirstClassLoader`. Issue #610 exposes that mechanism to library users.

## Assumed upstream API (issue #610)

```java
Oft oft = new OftRunner(List.of(pluginJar1, pluginJar2));
```

Threaded internally through `ServiceFactory` → `ImporterFactoryLoader` /
`ExporterFactoryLoader` / `ReporterFactoryLoader` → `InitializingServiceLoader.load(
serviceType, context, pluginJars)` → `ServiceLoaderFactory`, which merges the paths as
an additional `ServiceOrigin.forJars(pluginJars)`. The existing no-arg `OftRunner()`
delegates with an empty list, so the change is backward compatible.

> Confirm the exact signature and release version before implementing. If the final API
> differs (e.g. plugin paths on `ImportSettings`/`ReportSettings` instead of the
> `OftRunner` constructor), adjust step 3 accordingly.

## Steps

1. **Bump the OFT version.** In `build.gradle`, set `oftVersion` to the first release
   that contains the #610 API.
2. **Delete the class-loader package.** Remove
   `src/main/java/org/itsallcode/openfasttrace/gradle/task/classloader/`:
   - `OftPluginClassLoader.java`
   - `ChildFirstClassLoader.java`
   - `ParentClassLoader.java`
3. **Wire plugin files into `OftRunner`.**
   - `CollectTask.collectRequirements()`: replace
     `OftPluginClassLoader.runWithPlugins(pluginFiles, this::collectWithPlugins)` with a
     direct call to `collectWithPlugins()`, and construct the runner with plugin paths.
   - `TraceTask.trace()`: same for `traceWithPlugins()`.
   - Build the plugin path list from the resolved `pluginFiles` `ConfigurableFileCollection`,
     e.g. `pluginFiles.getFiles().stream().map(File::toPath).toList()`.
   - Remove the now-unused `OftPluginClassLoader` imports.
4. **Update documentation.**
   - `README.md` "Using OpenFastTrace Plugins": drop the class-loader/TCCL caveats and
     note that OFT handles plugin isolation.
   - `CHANGELOG.md`: add an entry under `[Unreleased]` referencing #610 and #77.
5. **Verify.**

## Files

| File | Change |
| --- | --- |
| `build.gradle` | Bump `oftVersion` |
| `src/main/java/.../task/CollectTask.java` | Use `new OftRunner(pluginPaths)`, drop wrapper |
| `src/main/java/.../task/TraceTask.java` | Use `new OftRunner(pluginPaths)`, drop wrapper |
| `src/main/java/.../task/classloader/*` | Delete (3 files) |
| `README.md` | Update plugin section |
| `CHANGELOG.md` | Add Unreleased entry |

## Verification

1. `./gradlew :test --tests org.itsallcode.openfasttrace.gradle.OpenFastTracePluginTest`
   — in particular `testTraceExampleProjectWithPluginDependency`.
2. Run the `plugin-config` example and confirm both the built-in `plain` report **and**
   the asciidoc plugin import work.
3. `./gradlew clean build --warning-mode fail -PenableConfigurationCache=true -PjavaVersion=25`
   (per repo memory: local JDK is Java 25; use `-PjavaVersion=25`).

## Risks / notes

- **Cross-repo dependency.** This plan is blocked until the OFT release containing #610
  is published. Do not merge the Gradle change before then.
- **API drift.** The assumed `OftRunner(List<Path>)` signature may change during upstream
  review; re-check #610 before implementing.
- **Child-first semantics.** OFT's `ChildFirstClassLoader` is fully child-first (no
  `api`/`core` parent-first exception, unlike the current Gradle loader). This is safe
  because the plugin developer guide forbids bundling `openfasttrace-api`; confirm this
  holds for the asciidoc plugin.
- **No fallback.** Per decision, the Gradle plugin will hard-require the new OFT version
  rather than keeping the TCCL-based loader for older versions.
