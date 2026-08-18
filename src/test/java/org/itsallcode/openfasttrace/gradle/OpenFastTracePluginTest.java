package org.itsallcode.openfasttrace.gradle;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.either;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.gradle.api.logging.Logging;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.testkit.runner.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;

@ParameterizedClass(name = "OpenFastTracePluginTest {0}")
@EnumSource(GradleTestConfig.class)
class OpenFastTracePluginTest
{
    private static final Logger LOG = Logging.getLogger(OpenFastTracePluginTest.class);

    private static final boolean ENABLE_WARNINGS = true;
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
        final BuildResult buildResult = runBuild(PROJECT_DEFAULT_CONFIG_DIR, "tasks");
        assertThat(buildResult.getOutput(), containsString(
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
        BuildResult buildResult = runBuild(projectDir, "tasks");
        assertThat(buildResult.getOutput(), containsString(
                "traceRequirements - Trace requirements and generate tracing report"));
        buildResult = runBuild(projectDir, "tasks");
        assertThat(buildResult.getOutput(),
                allOf(containsString(
                        "traceRequirements - Trace requirements and generate tracing report"),
                        containsString("Reusing configuration cache.")));
    }

    @Test
    void testTraceExampleProjectWithDefaultConfig()
    {
        final BuildResult buildResult = runBuild(PROJECT_DEFAULT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(PROJECT_DEFAULT_CONFIG_DIR.resolve("build/reports/tracing.txt"),
                "ok - 0 total");
    }

    @Test
    void testCollectExampleProjectWithCustomConfig()
    {
        final BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "collectRequirements");
        assertThat(buildResult.task(":collectRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(
                PROJECT_CUSTOM_CONFIG_DIR.resolve("build/reports/requirements.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
        BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "collectRequirements");
        assertThat(buildResult.task(":clean").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        assertThat(buildResult.task(":collectRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "collectRequirements");
        assertEquals(TaskOutcome.UP_TO_DATE,
                buildResult.task(":collectRequirements").getOutcome());
    }

    @Test
    void testHtmlReportConfig()
    {
        final BuildResult buildResult = runBuild(HTML_REPORT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(HTML_REPORT_CONFIG_DIR.resolve("build/reports/tracing.html"),
                "<!DOCTYPE html>",
                "<summary title=\"dsn~exampleB~1\"><span class=\"red\">&cross;</span>",
                "<details open>");
    }

    @Test
    void testTraceTaskUpToDateWhenAlreadyRun()
    {
        BuildResult buildResult = runBuild(HTML_REPORT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        buildResult = runBuild(HTML_REPORT_CONFIG_DIR, "traceRequirements");
        assertEquals(TaskOutcome.UP_TO_DATE,
                buildResult.task(":traceRequirements").getOutcome());
    }

    @Test
    void testTraceExampleProjectWithCustomConfig()
    {
        final BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void testTraceExampleProjectWithCustomConfigFailBuild()
    {
        final BuildResult buildResult = runBuildExpectFailure(PROJECT_CUSTOM_CONFIG_DIR,
                "clean", "traceRequirements", "-PfailBuild=true");
        assertEquals(TaskOutcome.FAILED,
                buildResult.task(":traceRequirements").getOutcome());
        TestUtil.assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredArtifactTypes()
    {
        final BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements", "-PfailBuild=true", "-PfilteredArtifactTypes=dsn");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
    }

    @Test
    void filteredWantedStatuses()
    {
        final BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements",
                "-PfilterWantedStatuses=draft,approved");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                "not ok [ in:  1 /  1 ✔ | out:  0 /  0   ] dsn~exampleB~1 [draft] (impl, -utest)",
                "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredWantedStatusesNoMatch()
    {
        final BuildResult buildResult = runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements",
                "-PfilterWantedStatuses=approved");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(
                PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                // Generated ID depends on JVM
                "not ok [ in:  0 /  0   | out:  0 /  1 ✘ ] impl~exampleB-",
                "not ok - 1 total, 1 direct, 0 transitive defects");
    }

    @Test
    void filteredWantedStatusesInvalidStatus()
    {
        final BuildResult buildResult = runBuildExpectFailure(PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements",
                "-PfilterWantedStatuses=invalid");
        assertThat(buildResult.getOutput(), containsString(
                "Invalid status 'invalid'. Valid statuses are: APPROVED, PROPOSED, DRAFT, REJECTED"));
    }

    @Test
    void testTraceExampleProjectWithCustomConfigFailBuildErrorMessage()
    {
        try
        {
            runBuild(PROJECT_CUSTOM_CONFIG_DIR, "clean", "traceRequirements",
                    "-PfailBuild=true");
        }
        catch (final UnexpectedBuildFailure e)
        {
            assertAll(
                    () -> assertEquals(TaskOutcome.FAILED,
                            e.getBuildResult()
                                    .task(":traceRequirements")
                                    .getOutcome()),
                    () -> assertThat(e.getMessage(),
                            startsWith("Unexpected build execution failure")),
                    () -> assertThat(e.getMessage(),
                            containsString("Requirement tracing found 1 defects. See report at")));
        }
    }

    @Test
    void testTraceMultiProject()
    {
        final BuildResult buildResult = runBuild(MULTI_PROJECT_DIR, "clean",
                "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(MULTI_PROJECT_DIR.resolve("build/custom-report.txt"),
                "ok - 6 total");
    }

    @Test
    void traceDependencyProject()
    {
        BuildResult buildResult = runBuild(DEPENDENCY_CONFIG_DIR, "clean");
        assertThat(buildResult.task(":clean").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.UP_TO_DATE)));
        final Path dependencyZip = DEPENDENCY_CONFIG_DIR
                .resolve("build/repo/requirements-1.0.zip");
        createDependencyZip(dependencyZip);

        buildResult = runBuild(DEPENDENCY_CONFIG_DIR, "traceRequirements");
        assertThat(buildResult.task(":traceRequirements").getOutcome(),
                either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        TestUtil.assertFileContent(
                DEPENDENCY_CONFIG_DIR.resolve("build/reports/tracing.txt"),
                "requirements-1.0.zip!spec.md:2",
                "requirements-1.0.zip!source.java:1",
                "not ok - 2 total, 1 direct, 0 transitive defects");
    }

    @Test
    void publishToMavenRepo()
    {
        final BuildResult buildResult = runBuild(PUBLISH_CONFIG_DIR, "clean",
                "publishToMavenLocal");
        assertEquals(TaskOutcome.SUCCESS,
                buildResult.task(":publishToMavenLocal").getOutcome());

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

    private BuildResult runBuildExpectFailure(final Path projectDir, final String... arguments)
    {
        return createGradleRunner(projectDir, arguments).buildAndFail();
    }

    private BuildResult runBuild(final Path projectDir, final String... arguments)
    {
        return createGradleRunner(projectDir, arguments).build();
    }

    private GradleRunner createGradleRunner(final Path projectDir, final String... arguments)
    {
        configureJacoco(projectDir);
        final List<String> allArgs = new ArrayList<>();
        allArgs.addAll(List.of(arguments));
        allArgs.addAll(List.of("--info", "--stacktrace", "--build-cache"));
        if (configurationCacheEnabled())
        {
            allArgs.add("--configuration-cache");
        }
        if (ENABLE_WARNINGS)
        {
            allArgs.addAll(List.of("--warning-mode", "all"));
        }
        final GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(allArgs)
                .forwardOutput();
        if (config.gradleVersion != null)
        {
            runner.withGradleVersion(config.gradleVersion);
        }
        return runner;
    }

    private static boolean configurationCacheEnabled()
    {
        return System.getProperty("enableConfigurationCache", "false")
                .equalsIgnoreCase("true");
    }

    private static void configureJacoco(final Path projectDir)
    {
        final Optional<String> testkitGradleConfig = TestUtil
                .readResource(OpenFastTracePluginTest.class,
                        "/testkit-gradle.properties");
        if (testkitGradleConfig.isEmpty())
        {
            LOG.info("Testkit gradle config not available. Skipping configuration");
            return;
        }
        LOG.info("Found testkit gradle config: {}", testkitGradleConfig.get());
        final Path gradleProperties = projectDir.resolve("gradle.properties");
        LOG.info("Writing testkit gradle config to {}", gradleProperties);
        TestUtil.writeFile(gradleProperties, testkitGradleConfig.get());
    }
}
