package org.itsallcode.openfasttrace.gradle;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import org.gradle.api.logging.Logging;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.gradle.internal.impldep.org.apache.commons.compress.archivers.zip.ZipFile;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;

class OpenFastTracePluginTest
{
    private static final Logger LOG = Logging.getLogger(OpenFastTracePluginTest.class);

    private static final boolean ENABLE_WARNINGS = true;
    private static final Path EXAMPLES_DIR = Paths.get("example-projects").toAbsolutePath();
    private static final Path PROJECT_DEFAULT_CONFIG_DIR = EXAMPLES_DIR.resolve("default-config");
    private static final Path PROJECT_CUSTOM_CONFIG_DIR = EXAMPLES_DIR.resolve("custom-config");
    private static final Path MULTI_PROJECT_DIR = EXAMPLES_DIR.resolve("multi-project");
    private static final Path DEPENDENCY_CONFIG_DIR = EXAMPLES_DIR.resolve("dependency-config");
    private static final Path PUBLISH_CONFIG_DIR = EXAMPLES_DIR.resolve("publish-config");
    private static final Path HTML_REPORT_CONFIG_DIR = EXAMPLES_DIR.resolve("html-report");

    @ParameterizedTest
    @EnumSource
    void testTracingTaskAddedToProject(final GradleTestConfig config)
    {
        final BuildResult buildResult = runBuild(config, PROJECT_DEFAULT_CONFIG_DIR, "tasks");
        assertThat(buildResult.getOutput(), containsString(
                "traceRequirements - Trace requirements and generate tracing report"));
    }

    @ParameterizedTest
    @EnumSource
    void testTraceExampleProjectWithDefaultConfig(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, PROJECT_DEFAULT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        assertFileContent(PROJECT_DEFAULT_CONFIG_DIR.resolve("build/reports/tracing.txt"),
                "ok - 0 total");
    }

    @ParameterizedTest
    @EnumSource
    void testCollectExampleProjectWithCustomConfig(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "collectRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":collectRequirements").getOutcome());
        assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/reports/requirements.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                        "<specdocument>", //
                "  <specobjects doctype=\"impl\">\n" + //
                        "    <specobject>\n" + //
                        "      <id>exampleB",
                "</id>\n" + //
                        "      <status>approved</status>\n" + //
                        "      <version>0</version>\n", //

                "      <sourceline>1</sourceline>\n" + //
                        "      <providescoverage>\n" + //
                        "        <provcov>\n" + //
                        "          <linksto>dsn:exampleB</linksto>\n" + //
                        "          <dstversion>1</dstversion>\n" + //
                        "        </provcov>\n" + //
                        "      </providescoverage>\n", //

                "  <specobjects doctype=\"dsn\">\n" + //
                        "    <specobject>\n" + //
                        "      <id>exampleB</id>\n" + //
                        "      <shortdesc>Tracing Example</shortdesc>\n" + //
                        "      <status>approved</status>\n" + //
                        "      <version>1</version>\n", //

                "      <sourceline>2</sourceline>\n" + //
                        "      <description>Example requirement</description>\n" + //
                        "      <needscoverage>\n" + //
                        "        <needsobj>utest</needsobj>\n" + //
                        "        <needsobj>impl</needsobj>\n" + //
                        "      </needscoverage>\n" + //
                        "    </specobject>\n", //

                "  </specobjects>\n" + //
                        "</specdocument>");
    }

    @ParameterizedTest
    @EnumSource
    void testCollectIsUpToDateWhenAlreadyRunBefore(final GradleTestConfig config) throws IOException
    {
        BuildResult buildResult = runBuild(config, PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "collectRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":collectRequirements").getOutcome());
        buildResult = runBuild(config, PROJECT_CUSTOM_CONFIG_DIR, "collectRequirements");
        assertEquals(TaskOutcome.UP_TO_DATE, buildResult.task(":collectRequirements").getOutcome());
    }

    @ParameterizedTest
    @EnumSource
    void testHtmlReportConfig(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, HTML_REPORT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        assertFileContent(HTML_REPORT_CONFIG_DIR.resolve("build/reports/tracing.html"),
                "<!DOCTYPE html>", //
                "<summary title=\"dsn~exampleB~1\"><span class=\"red\">&cross;</span>");
    }

    @ParameterizedTest
    @EnumSource
    void testTraceTaskUpToDateWhenAlreadyRun(final GradleTestConfig config) throws IOException
    {
        BuildResult buildResult = runBuild(config, HTML_REPORT_CONFIG_DIR, "clean",
                "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        buildResult = runBuild(config, HTML_REPORT_CONFIG_DIR, "traceRequirements");
        assertEquals(TaskOutcome.UP_TO_DATE, buildResult.task(":traceRequirements").getOutcome());
    }

    @ParameterizedTest
    @EnumSource
    void testTraceExampleProjectWithCustomConfig(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, PROJECT_CUSTOM_CONFIG_DIR, "clean",
                "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                "not ok - 0/1>0>0/0 - dsn~exampleB~1 (impl, -utest)", //
                "not ok - 2 total, 1 defect");
    }

    @ParameterizedTest
    @EnumSource
    void testTraceMultiProject(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, MULTI_PROJECT_DIR, "clean",
                "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        assertFileContent(MULTI_PROJECT_DIR.resolve("build/custom-report.txt"), "ok - 6 total");
    }

    @ParameterizedTest
    @EnumSource
    void testTraceDependencyProject(final GradleTestConfig config) throws IOException
    {
        BuildResult buildResult = runBuild(config, DEPENDENCY_CONFIG_DIR, "clean");
        final Path dependencyZip = DEPENDENCY_CONFIG_DIR.resolve("build/repo/requirements-1.0.zip");
        createDependencyZip(dependencyZip);

        buildResult = runBuild(config, DEPENDENCY_CONFIG_DIR, "traceRequirements");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":traceRequirements").getOutcome());
        assertFileContent(DEPENDENCY_CONFIG_DIR.resolve("build/reports/tracing.txt"),
                "requirements-1.0.zip!spec.md:2", //
                "requirements-1.0.zip!source.java:1", //
                "not ok - 2 total, 1 defect");
    }

    @ParameterizedTest
    @EnumSource
    void testPublishToMavenRepo(final GradleTestConfig config) throws IOException
    {
        final BuildResult buildResult = runBuild(config, PUBLISH_CONFIG_DIR, "clean",
                "publishToMavenLocal");
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(":publishToMavenLocal").getOutcome());

        final Path archive = PUBLISH_CONFIG_DIR
                .resolve("build/distributions/publish-config-1.0.zip");
        assertTrue(Files.exists(archive));
        try (ZipFile zip = new ZipFile(archive.toFile()))
        {
            final String entryContent = readEntry(zip, "requirements.xml");
            assertThat(entryContent, containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                    "<specdocument>\n"));
            assertThat(entryContent, containsString("  <specobjects doctype=\"dsn\">\n" + //
                    "    <specobject>\n" + //
                    "      <id>exampleB</id>\n" + //
                    "      <shortdesc>Tracing Example</shortdesc>\n" + //
                    "      <status>approved</status>\n" + //
                    "      <version>1</version>"));
            assertThat(entryContent, containsString("      <sourceline>2</sourceline>\n" + //
                    "      <description>Example requirement</description>\n" + //
                    "      <needscoverage>\n" + //
                    "        <needsobj>utest</needsobj>\n" + //
                    "        <needsobj>impl</needsobj>\n" + //
                    "      </needscoverage>\n" + //
                    "    </specobject>\n"));
        }
    }

    private static String readEntry(final ZipFile zip, final String entryName)
            throws IOException, ZipException
    {
        final ZipArchiveEntry reqirementsEntry = zip.getEntry(entryName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(zip.getInputStream(reqirementsEntry))))
        {
            return reader.lines().collect(joining("\n"));
        }
    }

    private static void createDependencyZip(final Path dependencyZip) throws IOException
    {
        Files.createDirectories(dependencyZip.getParent());
        try (ZipFileBuilder zipBuilder = ZipFileBuilder.create(dependencyZip))
        {
            zipBuilder
                    .addEntry("source.java", PROJECT_DEFAULT_CONFIG_DIR.resolve("src/source.java")) //
                    .addEntry("spec.md", PROJECT_DEFAULT_CONFIG_DIR.resolve("doc/spec.md"));
        }
    }

    private static void assertFileContent(final Path file, final String... lines) throws IOException
    {
        final String fileContent = fileContent(file);
        for (final String line : lines)
        {
            assertThat("Content of file " + file, fileContent, containsString(line));
        }
    }

    private static String fileContent(final Path file) throws IOException
    {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static BuildResult runBuild(final GradleTestConfig config, final Path projectDir,
            final String... arguments)
    {
        assumeTrue(config.supportedWithJvm());
        configureJacoco(projectDir);
        final List<String> allArgs = new ArrayList<>();
        allArgs.addAll(asList(arguments));
        allArgs.addAll(asList("--info", "--stacktrace"));
        if (ENABLE_WARNINGS)
        {
            allArgs.addAll(asList("--warning-mode", "all"));
        }
        final GradleRunner runner = GradleRunner.create() //
                .withProjectDir(projectDir.toFile()) //
                .withPluginClasspath() //
                .withArguments(allArgs) //
                .forwardOutput();
        if (config.gradleVersion != null)
        {
            runner.withGradleVersion(config.gradleVersion);
        }
        return runner.build();
    }

    private static void configureJacoco(final Path projectDir)
    {
        final String testkitGradleConfig = TestUtil.readResource(OpenFastTracePluginTest.class,
                "/testkit-gradle.properties");
        LOG.info("Found testkit gradle config: {}", testkitGradleConfig);
        final Path gradleProperties = projectDir.resolve("gradle.properties");
        LOG.info("Writing testkit gradle config to {}", gradleProperties);
        TestUtil.writeFile(gradleProperties, testkitGradleConfig);
    }
}
