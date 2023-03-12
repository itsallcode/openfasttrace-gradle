# openfasttrace-gradle
Gradle plugin for the requirement tracing suite [OpenFastTrace](https://github.com/itsallcode/openfasttrace).

## Project Information

[![Java CI with Gradle](https://github.com/itsallcode/openfasttrace-gradle/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/itsallcode/openfasttrace-gradle/actions?query=workflow%3A%22Java+CI+with+Gradle%22)
[![Sonarcloud Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.itsallcode%3Aopenfasttrace-gradle&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.itsallcode%3Aopenfasttrace-gradle)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.itsallcode%3Aopenfasttrace-gradle&metric=coverage)](https://sonarcloud.io/dashboard?id=org.itsallcode%3Aopenfasttrace-gradle)

* [Blog](https://blog.itsallcode.org/)
* [Changelog](CHANGELOG.md)
* [Contributing guide](CONTRIBUTING.md)
* [OpenFastTrace stories](https://github.com/itsallcode/openfasttrace/wiki/OFT-Stories)

## Usage

1. Preconditions: Java 11 and Gradle 7.0
1. Add plugin [`org.itsallcode.openfasttrace`](https://plugins.gradle.org/plugin/org.itsallcode.openfasttrace) to your project:

    ```gradle
    plugins {
      id "org.itsallcode.openfasttrace" version "1.1.0"
    }
    ```

1. Configure your project, see [examples](https://github.com/itsallcode/openfasttrace-gradle/tree/main/example-projects)
1. Run

    ```bash
    ./gradlew traceRequirements
    ```

1. Report is written to `build/reports/tracing.txt` by default.

### Configuring the short tag importer

The short tag importer allows omitting artifact type and the covered artifact type. Optionally you can add a prefix to the item name, e.g. a common module name.

```gradle
requirementTracing {
  tags {
    tag {
      paths = fileTree(dir: 'src/').include '*.impl.*'
      coveredItemNamePrefix = 'prefix'
      tagArtifactType = 'impl'
      coveredItemArtifactType = 'dsn'
    }
    tag {
      paths = fileTree(dir: 'src/').include '*.test.*'
      tagArtifactType = 'utest'
      coveredItemArtifactType = 'dsn'
    }
  }
}
```

As a benefit the tags are much shorter and contain only the name and revision:

```java
// [[tagname:1]]
```

See [multi-project/sub1](https://github.com/itsallcode/openfasttrace-gradle/tree/main/example-projects/multi-project/sub1) for a basic example.

### Sharing requirements

In bigger setups you might want to share requirements between multiple projects.

Example: The Software Architecture Design project `swad` contains overall requirements that must be fulfilled by projects `component-a` and `component-b`.

1. The `swad` project publishes its requirements as a zip file `swad-req` to a maven repository.
1. Both components import these requirements and cover them in their Software Detailed Design (swdd).
1. Both components publish their requirements as artefacts `component-a-req` and `component-b-req` to the shared maven repository.
1. A regular job check that all requirements from `swad` are covered by tracing `swad-req`, `component-a-req` and `component-b-req`.

#### Publishing requirements to a maven repository

If you want to publish requirements to a maven repository you can use the following configuration in your `build.gradle`:

```gradle
plugins {
  id 'org.itsallcode.openfasttrace'
  id 'maven-publish'
}

requirementTracing {
  inputDirectories = files('doc')
}

task requirementsZip(type: Zip, dependsOn: collectRequirements) {
 from collectRequirements.outputFile
 into '/'
}

publishing {
  publications {
    maven(MavenPublication) {
      artifact requirementsZip
    }
  }
}
```

See [publish-config](https://github.com/itsallcode/openfasttrace-gradle/tree/main/example-projects/publish-config) for a basic example.

#### Importing external requirements

You can import requirements from another project using the `importedRequirements` configuration. The requirements must be published to a repository as a zip file and can be referenced using the usual gradle dependency syntax:

```gradle
repositories {
  maven {
    url "http://repo.example.com/maven2"
  }
}
requirementTracing {
  importedRequirements = ['com.example:swad:1.0.0@zip']
}
```

See [dependency-config](https://github.com/itsallcode/openfasttrace-gradle/tree/main/example-projects/dependency-config) for a basic example.

## Development

```bash
git clone https://github.com/itsallcode/openfasttrace-gradle-gradle.git
./gradlew check
# Test report: build/reports/tests/index.html
```

### Use `openfasttrace` from source

To use `openfasttrace` from source during development:

1. Clone https://github.com/itsallcode/openfasttrace to `../openfasttrace`
1. Create file `gradle.properties` with the following content:

    ```properties
    oftSourceDir = ../openfasttrace
    ```

### Using eclipse

Import into eclipse using [buildship](https://projects.eclipse.org/projects/tools.buildship) plugin:

1. Select File > Import... > Gradle > Gradle Project
1. Click "Next"
1. Select Project root directory
1. Click "Finish"

### Check if dependencies are up-to-date

```bash
./gradlew dependencyUpdates
```

### Check dependencies for vulnerabilities

```bash
./gradlew ossIndexAudit
```

### Run local sonar analysis

```bash
./gradlew clean sonar --info \
    -Dsonar.host.url=https://sonarcloud.io \
    -Dsonar.organization=itsallcode \
    -Dsonar.login=[token]
```

### Publish to `plugins.gradle.org`

#### Preparations

Add your API key to `~/.gradle/gradle.properties`:

```properties
gradle.publish.key = <key>
gradle.publish.secret = <secret>
```

#### Publish release

1. Make sure that property `oftSourceDir` in file `gradle.properties` is commented out, i.e. OpenFastTrace is not used from source.
1. Update version number in `build.gradle` and `README.md`.
1. Add changes in new version to `CHANGELOG.md`.
1. Commit and push changes.
1. Run

    ```bash
    ./gradlew clean publishPlugins --info
    ```

   Plugin will be published at https://plugins.gradle.org/m2/org/itsallcode/openfasttrace/org.itsallcode.openfasttrace.gradle.plugin/
1. Create a [release](https://github.com/itsallcode/openfasttrace-gradle/releases) in GitHub
