# openfasttrack-gradle
Gradle plugin for [OpenFastTrack](https://github.com/hamstercommunity/openfasttrack)

[![Build Status](https://travis-ci.org/hamstercommunity/openfasttrack-gradle.svg?branch=develop)](https://travis-ci.org/hamstercommunity/openfasttrack-gradle)
[![Sonarcloud Quality Gate](https://sonarcloud.io/api/badges/gate?key=com.github.kaklakariada%3Aopenfasttrack-gradle%3Adevelop)](https://sonarcloud.io/dashboard/index/com.github.kaklakariada%3Aopenfasttrack-gradle%3Adevelop)
[![codecov](https://codecov.io/gh/hamstercommunity/openfasttrack-gradle/branch/develop/graph/badge.svg)](https://codecov.io/gh/hamstercommunity/openfasttrack-gradle)

## Development

```bash
$ git clone https://github.com/hamstercommunity/openfasttrack-gradle.git
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
