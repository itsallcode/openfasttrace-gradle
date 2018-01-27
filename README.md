# openfasttrace-gradle
Gradle plugin for [OpenFastTrace](https://github.com/itsallcode/openfasttrace)

[![Build Status](https://travis-ci.org/itsallcode/openfasttrace-gradle.svg?branch=develop)](https://travis-ci.org/itsallcode/openfasttrace-gradle)
[![Sonarcloud Quality Gate](https://sonarcloud.io/api/badges/gate?key=org.itsallcode%3Aopenfasttrace-gradle%3Adevelop)](https://sonarcloud.io/dashboard?id=org.itsallcode%3Aopenfasttrace-gradle%3Adevelop)
[![codecov](https://codecov.io/gh/itsallcode/openfasttrace-gradle/branch/develop/graph/badge.svg)](https://codecov.io/gh/itsallcode/openfasttrace-gradle)

## Development

```bash
$ git clone https://github.com/itsallcode/openfasttrace-gradle-gradle.git
$ ./gradlew check
# Test report: build/reports/tests/index.html
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

1. Add your API key to `~/.gradle/gradle.properties`:

    ```
    gradle.publish.key = <key>
    gradle.publish.secret = <secret>
    ```

1. Update version number in `build.gradle`
1. Run

    ```bash
    $ ./gradlew clean publishPlugins --info
    ```

1. Create a [release](https://github.com/itsallcode/openfasttrace-gradle/releases) in GitHub
