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

1. Preconditions: Java 17 and Gradle 8.6
1. Add plugin [`org.itsallcode.openfasttrace`](https://plugins.gradle.org/plugin/org.itsallcode.openfasttrace) to your project:

    ```groovy
    plugins {
      id "org.itsallcode.openfasttrace" version "3.1.0"
    }
    ```

1. Configure your project, see [examples](https://github.com/itsallcode/openfasttrace-gradle/tree/main/example-projects)
1. Run

    ```sh
    ./gradlew traceRequirements
    ```

1. Report is written to `build/reports/tracing.txt` by default.

### General Configuration

```groovy
requirementTracing {
  failBuild = true
  inputDirectories = files('custom-dir')
  reportFile = file('build/custom-report.txt')
  reportFormat = 'plain'
  reportVerbosity = 'failure_details'
  detailsSectionDisplay = 'collapse'
  filteredArtifactTypes = ["req", "dsn"]
}
```

You can configure the following properties:

* `failBuild`: Fail build when tracing finds any issues (default: `true`)
* `inputDirectories`: Files or directories to import
* `reportFile`: Path to the report file
* `reportFormat`: Format of the report
  * `plain` - Plain Text (default)
  * `html` - HTML
* `reportVerbosity`: Report verbosity
  * `quiet` - no output (in case only the return code is used)
  * `minimal` - display ok or not ok
  * `summary` - display only the summary, not individual specification items
  * `failures` - list of defect specification items
  * `failure_summaries` - list of summaries for defect specification items
  * `failure_details` - summaries and details for defect specification items (default)
  * `all` - summaries and details for all specification items
* `detailsSectionDisplay`: Initial display status of the details section in the HTML report
  * `collapse` - hide details (default)
  * `expand` - show details
* `filteredArtifactTypes`: Use only the listed artifact types during tracing

### Configuring the Short Tag Importer

The short tag importer allows omitting artifact type and the covered artifact type. Optionally you can add a prefix to the item name, e.g. a common module name.

```groovy
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

### Sharing Requirements

In bigger setups you might want to share requirements between multiple projects.

Example: The Software Architecture Design project `swad` contains overall requirements that must be fulfilled by projects `component-a` and `component-b`.

1. The `swad` project publishes its requirements as a zip file `swad-req` to a Maven repository.
1. Both components import these requirements and cover them in their Software Detailed Design (swdd).
1. Both components publish their requirements as artefacts `component-a-req` and `component-b-req` to the shared Maven repository.
1. A regular job check that all requirements from `swad` are covered by tracing `swad-req`, `component-a-req` and `component-b-req`.

#### Publishing Requirements to a Maven Repository

If you want to publish requirements to a Maven repository you can use the following configuration in your `build.gradle`:

```groovy
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

#### Importing External Requirements

You can import requirements from another project using the `importedRequirements` configuration. The requirements must be published to a repository as a zip file and can be referenced using the usual gradle dependency syntax:

```groovy
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

```sh
git clone https://github.com/itsallcode/openfasttrace-gradle.git
cd openfasttrace-gradle
./gradlew check
# Test report: build/reports/tests/index.html
```

### Use `openfasttrace` from Source

To use `openfasttrace` from source during development:

1. Clone https://github.com/itsallcode/openfasttrace to `../openfasttrace`
1. Create file `gradle.properties` with the following content:

    ```properties
    oftSourceDir = ../openfasttrace
    ```

### Check if dependencies are up-to-date

```sh
./gradlew dependencyUpdates
```

### Check dependencies for vulnerabilities

Get token for OssIndex from [ossindex.sonatype.org](https://ossindex.sonatype.org/user/settings) and add it to `~/.gradle/gradle.properties`:

```properties
ossIndexUsername = <user>
ossIndexToken    = <token>
```

Then run

```sh
./gradlew ossIndexAudit
```

### Run sonar analysis

```sh
./gradlew clean sonar --info -Dsonar.token=[token]
```

### Publish to `plugins.gradle.org`

#### Preparations

1. Checkout the `main` branch, create a new branch.
2. Update version number in `build.gradle` and `README.md`.
3. Add changes in new version to `CHANGELOG.md`.
4. Commit and push changes.
5. Create a new pull request, have it reviewed and merged to `main`.

#### Perform the Release

1. Start the release workflow
  * Run command `gh workflow run release.yml --repo itsallcode/openfasttrace-gradle --ref main`
  * or go to [GitHub Actions](https://github.com/itsallcode/openfasttrace-gradle/actions/workflows/release.yml) and start the `release.yml` workflow on branch `main`.
2. Update title and description of the newly created [GitHub release](https://github.com/itsallcode/openfasttrace-gradle/releases).
3. Plugin will be published at https://plugins.gradle.org/m2/org/itsallcode/openfasttrace/org.itsallcode.openfasttrace.gradle.plugin/
