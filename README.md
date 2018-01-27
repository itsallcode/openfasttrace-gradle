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
2. Click "Next"
3. Select Project root directory
4. Click "Finish"

### Generate license header for added files:

```bash
$ ./gradlew licenseFormat
```
