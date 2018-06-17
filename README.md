# openfasttrace-gradle
Gradle plugin for [OpenFastTrace](https://github.com/itsallcode/openfasttrace)

[![Build Status](https://travis-ci.org/itsallcode/openfasttrace-gradle.svg?branch=develop)](https://travis-ci.org/itsallcode/openfasttrace-gradle)
[![Sonarcloud Quality Gate](https://sonarcloud.io/api/badges/gate?key=org.itsallcode%3Aopenfasttrace-gradle%3Adevelop)](https://sonarcloud.io/dashboard?id=org.itsallcode%3Aopenfasttrace-gradle%3Adevelop)
[![codecov](https://codecov.io/gh/itsallcode/openfasttrace-gradle/branch/develop/graph/badge.svg)](https://codecov.io/gh/itsallcode/openfasttrace-gradle)

## Usage

1. Add plugin [`org.itsallcode.openfasttrace`](https://plugins.gradle.org/plugin/org.itsallcode.openfasttrace) to your project:

    ```gradle
    plugins {
      id "org.itsallcode.openfasttrace" version "0.1.0"
    }
    ```

1. Configure project, see [example projects](https://github.com/itsallcode/openfasttrace-gradle/tree/develop/example-projects)
1. Run

    ```bash
    $ ./gradlew traceRequirements
    ```

1. Report is written to `build/reports/tracing.txt` by default.

## Development

```bash
$ git clone https://github.com/itsallcode/openfasttrace-gradle-gradle.git
$ ./gradlew check
# Test report: build/reports/tests/index.html
```

### Use `openfasttrace` from source

To use `openfasttrace` from source during development create file `gradle.properties` with the following content:

```properties
oftSourceDir = ../openfasttrace
```

### Using eclipse

Import into eclipse using [buildship](https://projects.eclipse.org/projects/tools.buildship) plugin:

1. Select File > Import... > Gradle > Gradle Project
1. Click "Next"
1. Select Project root directory
1. Click "Finish"

### Generate license header for added files:

```bash
$ ./gradlew licenseFormat
```

### Publish to `plugins.gradle.org`

#### Preparations

Add your API key to `~/.gradle/gradle.properties`:

    ```properties
    gradle.publish.key = <key>
    gradle.publish.secret = <secret>
    ```

#### Publish release

1. Update version number in `build.gradle` and `README.md`
1. Commit and push changes
1. Run

    ```bash
    $ ./gradlew clean publishPlugins
    ```

   Plugin will be published at https://plugins.gradle.org/m2/org/itsallcode/openfasttrace/org.itsallcode.openfasttrace.gradle.plugin/
1. Create a [release](https://github.com/itsallcode/openfasttrace-gradle/releases) in GitHub
