package org.itsallcode.openfasttrace.gradle;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.*;
import java.nio.file.*;

import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

@ParameterizedClass(name = "OpenFastTracePluginTest {0}")
@EnumSource(GradleTestConfig.class)
class OpenFastTracePluginTest
{
    private static final Path EXAMPLES_DIR = Paths.get("example-projects").toAbsolutePath();
    private static final Path PROJECT_DEFAULT_CONFIG_DIR = EXAMPLES_DIR
            .resolve("default-config");
    private static final Path PROJECT_CUSTOM_CONFIG_DIR = EXAMPLES_DIR.resolve("custom-config");
    private static final Path MULTI_PROJECT_DIR = EXAMPLES_DIR.resolve("multi-project");
    private static final Path DEPENDENCY_CONFIG_DIR = EXAMPLES_DIR.resolve("dependency-config");
    private static final Path PUBLISH_CONFIG_DIR = EXAMPLES_DIR.resolve("publish-config");
    private static final Path HTML_REPORT_CONFIG_DIR = EXAMPLES_DIR.resolve("html-report");

    @Parameter
    private GradleTestConfig config;

    @Test
    void tracingTaskAddedToProject()
    {
        fixture(PROJECT_DEFAULT_CONFIG_DIR).withArgs("tasks").run()
                .assertOutput(containsString(
                        "traceRequirements - Trace requirements and generate tracing report"));
    }

    @Test
    void pluginUsesConfigurationCache()
    {
        testConfigurationCache(PROJECT_DEFAULT_CONFIG_DIR);
    }

    @Test
    void pluginUsesConfigurationCacheWithMultiModuleProject()
    {
        testConfigurationCache(MULTI_PROJECT_DIR);
    }

    private void testConfigurationCache(final Path projectDir)
    {
        assumeTrue(configurationCacheEnabled(), "Configuration cache is not enabled");
        final PluginTestFixture fixture = fixture(projectDir).withArgs("tasks");

        fixture.run().assertOutput(containsString(
                "traceRequirements - Trace requirements and generate tracing report"));

        fixture.run().assertOutput(allOf(containsString(
                "traceRequirements - Trace requirements and generate tracing report"),
                containsString("Reusing configuration cache.")));
    }

    @Test
    void testTraceExampleProjectWithDefaultConfig()
    {
        fixture(PROJECT_DEFAULT_CONFIG_DIR).withArgs("clean", "traceRequirements")
                .withReportFile(Path.of("build/reports/tracing.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines("ok - 0 total");
    }

    @Test
    void testCollectExampleProjectWithCustomConfig()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR).withArgs("clean", "collectRequirements")
                .withReportFile(Path.of("build/reports/requirements.xml"))
                .run().assertCollectOutcomeSuccessOrFromCache()
                .assertReportFileLines("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<specdocument>",
                        """
                                  <specobjects doctype="impl">
                                    <specobject>
                                      <id>exampleB\
                                """, """
                                </id>
                                      <status>approved</status>
                                      <version>0</version>
                                """,

                        """
                                      <sourceline>1</sourceline>
                                      <providescoverage>
                                        <provcov>
                                          <linksto>dsn:exampleB</linksto>
                                          <dstversion>1</dstversion>
                                        </provcov>
                                      </providescoverage>
                                """,

                        """
                                  <specobjects doctype="dsn">
                                    <specobject>
                                      <id>exampleB</id>
                                      <shortdesc>Tracing Example</shortdesc>
                                      <status>draft</status>
                                      <version>1</version>
                                """,

                        """
                                      <sourceline>2</sourceline>
                                      <description>Example requirement</description>
                                      <needscoverage>
                                        <needsobj>utest</needsobj>
                                        <needsobj>impl</needsobj>
                                      </needscoverage>
                                    </specobject>
                                """,

                        "  </specobjects>\n" +
                                "</specdocument>");
    }

    @Test
    void testCollectIsUpToDateWhenAlreadyRunBefore()
    {
        final PluginTestFixture fixture = fixture(PROJECT_CUSTOM_CONFIG_DIR);
        fixture.withArgs("clean", "collectRequirements").run()
                .assertCollectOutcomeSuccessOrFromCache()
                .assertOutcomeSuccessOrFromCache(":clean")
                .assertCollectOutcomeSuccessOrFromCache();

        fixture.withArgs("collectRequirements").run()
                .assertOutcome(":collectRequirements", TaskOutcome.UP_TO_DATE);
    }

    @Test
    void testHtmlReportConfig()
    {
        fixture(HTML_REPORT_CONFIG_DIR)
                .withArgs("clean", "traceRequirements")
                .withReportFile(Path.of("build/reports/tracing.html"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines("<!DOCTYPE html>",
                        "<summary title=\"dsn~exampleB~1\"><span class=\"red\">&cross;</span>",
                        "<details open>");
    }

    @Test
    void testTraceTaskUpToDateWhenAlreadyRun()
    {
        final PluginTestFixture fixture = fixture(HTML_REPORT_CONFIG_DIR);
        fixture.withArgs("clean", "traceRequirements").run()
                .assertTraceOutcomeSuccessOrFromCache();
        fixture.withArgs("traceRequirements").run().assertOutcome(":traceRequirements",
                TaskOutcome.UP_TO_DATE);
    }

    @Test
    void testTraceExampleProjectWithCustomConfig()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR).withArgs("clean", "traceRequirements")
                .withReportFile(Path.of("build/custom-report.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines(
                        "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                        "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void testTraceExampleProjectWithCustomConfigFailBuild()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements", "-PfailBuild=true")
                .withReportFile(Path.of("build/custom-report.txt"))
                .runExpectingFailure()
                .assertOutcome(":traceRequirements", TaskOutcome.FAILED)
                .assertReportFileLines(
                        "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                        "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredArtifactTypes()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements", "-PfailBuild=true",
                        "-PfilteredArtifactTypes=dsn")
                .run()
                .assertTraceOutcomeSuccessOrFromCache();
    }

    @Test
    void filteredWantedStatuses()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements",
                        "-PfilterWantedStatuses=draft,approved")
                .withReportFile(Path.of("build/custom-report.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines(
                        "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                        "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredWantedStatusesNoMatch()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements", "-PfilterWantedStatuses=approved")
                .withReportFile(Path.of("build/custom-report.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines(
                        // Generated ID depends on JVM
                        "not ok [ in:  0 /  0   | out:  0 /  1 ✘ ] impl~exampleB-",
                        "not ok - 1 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredWantedStatusesInvalidStatus()
    {
        fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements", "-PfilterWantedStatuses=invalid")
                .runExpectingFailure()
                .assertOutput(containsString(
                        "Invalid status 'invalid'. Valid statuses are: APPROVED, PROPOSED, DRAFT, REJECTED"));
    }

    @Test
    void testTraceExampleProjectWithCustomConfigFailBuildErrorMessage()
    {
        final PluginTestFixture fixture = fixture(PROJECT_CUSTOM_CONFIG_DIR)
                .withArgs("clean", "traceRequirements", "-PfailBuild=true");
        final UnexpectedBuildFailure exception = assertThrows(UnexpectedBuildFailure.class,
                fixture::run);
        assertAll(
                () -> assertEquals(TaskOutcome.FAILED,
                        exception.getBuildResult().task(":traceRequirements").getOutcome()),
                () -> assertThat(exception.getMessage(),
                        startsWith("Unexpected build execution failure")),
                () -> assertThat(exception.getMessage(),
                        containsString("Requirement tracing found 1 defects. See report at")));
    }

    @Test
    void testTraceMultiProject()
    {
        fixture(MULTI_PROJECT_DIR).withArgs("clean", "traceRequirements")
                .withReportFile(Path.of("build/custom-report.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines("ok - 6 total");
    }

    @Test
    void traceDependencyProject()
    {
        fixture(DEPENDENCY_CONFIG_DIR).withArgs("clean").run()
                .assertOutcome(":clean", TaskOutcome.SUCCESS);
        final Path dependencyZip = DEPENDENCY_CONFIG_DIR
                .resolve("build/repo/requirements-1.0.zip");
        createDependencyZip(dependencyZip);

        fixture(DEPENDENCY_CONFIG_DIR).withArgs("traceRequirements")
                .withReportFile(Path.of("build/reports/tracing.txt"))
                .run()
                .assertTraceOutcomeSuccessOrFromCache()
                .assertReportFileLines(
                        "requirements-1.0.zip!spec.md:2",
                        "requirements-1.0.zip!source.java:1",
                        "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void publishToMavenRepo()
    {
        fixture(PUBLISH_CONFIG_DIR).withArgs("clean", "publishToMavenLocal")
                .run()
                .assertOutcome(":publishToMavenLocal", TaskOutcome.SUCCESS);

        final Path archive = PUBLISH_CONFIG_DIR
                .resolve("build/distributions/publish-config-1.0.zip");
        assertTrue(Files.exists(archive));
        try (ZipFile zip = ZipFile.builder().setFile(archive.toFile()).get())
        {
            final String entryContent = readEntry(zip, "requirements.xml");
            assertThat(entryContent, containsString("""
                    <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                    <specdocument>
                    """));
            assertThat(entryContent, containsString("""
                      <specobjects doctype="dsn">
                        <specobject>
                          <id>exampleB</id>
                          <shortdesc>Tracing Example</shortdesc>
                          <status>approved</status>
                          <version>1</version>\
                    """));
            assertThat(entryContent, containsString("""
                          <sourceline>2</sourceline>
                          <description>Example requirement</description>
                          <needscoverage>
                            <needsobj>utest</needsobj>
                            <needsobj>impl</needsobj>
                          </needscoverage>
                        </specobject>
                    """));
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException("Failed to read zip file " + archive, e);
        }
    }

    private static String readEntry(final ZipFile zip, final String entryName)
    {
        final ZipArchiveEntry reqirementsEntry = zip.getEntry(entryName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(zip.getInputStream(reqirementsEntry))))
        {
            return reader.lines().collect(joining("\n"));
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException("Failed to read entry " + entryName, e);
        }
    }

    private static void createDependencyZip(final Path dependencyZip)
    {
        TestUtil.createDirs(dependencyZip.getParent());
        try (ZipFileBuilder zipBuilder = ZipFileBuilder.create(dependencyZip))
        {
            zipBuilder
                    .addEntry("source.java",
                            PROJECT_DEFAULT_CONFIG_DIR
                                    .resolve("src/source.java")) //
                    .addEntry("spec.md", PROJECT_DEFAULT_CONFIG_DIR
                            .resolve("doc/spec.md"));
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(
                    "Failed to create dependency zip " + dependencyZip, e);
        }
    }

    private static boolean configurationCacheEnabled()
    {
        return System.getProperty("enableConfigurationCache", "false")
                .equalsIgnoreCase("true");
    }

    private PluginTestFixture fixture(final Path projectDir)
    {
        return PluginTestFixture.create(config, projectDir);
    }
}
