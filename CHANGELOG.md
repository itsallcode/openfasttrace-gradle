# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- [PR #24](https://github.com/itsallcode/openfasttrace-gradle/pull/24):
  - Upgrade to [OpenFastTrace 3.2.1](https://github.com/itsallcode/openfasttrace/releases/tag/3.2.1)
  - Upgrade other dependencies
  - Build and test with Java 16. Please note that Java 16 is only supported with Gradle 7.0+.
- [PR #25](https://github.com/itsallcode/openfasttrace-gradle/pull/25): Upgrade build to Gradle 7.2, test with 7.0.
- [PR #32](https://github.com/itsallcode/openfasttrace-gradle/pull/32): Upgrade dependencies
  - Upgrade to [OpenFastTrace 3.5.0](https://github.com/itsallcode/openfasttrace/releases/tag/3.5.0)
  - Gradle 6.0 is no longer supported.
  - Build and test with Java 17. Please note that Java 17 is only supported with Gradle 7.5+.

## [0.9.0] - 2021-05-30

### Changed

- Upgrade to [OpenFastTrace 3.2.0](https://github.com/itsallcode/openfasttrace/releases/tag/3.2.0)

## [0.8.0] - 2021-05-22

### Changed

- [#19](https://github.com/itsallcode/openfasttrace-gradle/pull/19) Upgrade to [OpenFastTrace 3.1.0](https://github.com/itsallcode/openfasttrace/releases/tag/3.1.0), adding support for JVM languages Clojure, Kotlin and Scala.
- Upgrade to Gradle 7.0.2, tested with Gradle 6.0

## [0.7.0] - 2019-05-24

### Changed

- Upgrade to OpenFastTrace 3.0.2
- Requires Java 11 and Gradle 6.0

## [0.6.2] - 2019-04-06

### Changed

- Upgrade to OpenFastTrace 2.3.5. This includes the following fixes:
    - Changed: Pretty print specobjects [#229](https://github.com/itsallcode/openfasttrace/pull/229) [#219](https://github.com/itsallcode/openfasttrace/issues/219)
    - Fixed: Export specobject title [#212](https://github.com/itsallcode/openfasttrace/pull/212/) [#209](https://github.com/itsallcode/openfasttrace/issues/209)
    - Fixed: Fix exit codes [#218](https://github.com/itsallcode/openfasttrace/pull/218) [#215](https://github.com/itsallcode/openfasttrace/issues/215)
- Upgrade to Gradle 5.3.1 (Gradle 4.x is still supported)

## [0.6.1] - 2019-04-06 [YANKED]

## [0.6.0] - 2018-12-12

### Changed

- Upgrade to OpenFastTrace 2.2.0
- Upgrade to Gradle 5.0 (Gradle 4.x is still supported)
