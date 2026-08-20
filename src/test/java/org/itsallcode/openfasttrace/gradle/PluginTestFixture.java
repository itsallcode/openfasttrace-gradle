package org.itsallcode.openfasttrace.gradle;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.*;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.testkit.runner.*;
import org.hamcrest.Matcher;

class PluginTestFixture
{
    private static final Logger LOG = Logging.getLogger(PluginTestFixture.class);

    private final GradleTestConfig config;
    private final Path projectDir;
    private String[] arguments;
    private Path relativeReportPath;

    private PluginTestFixture(final GradleTestConfig config, final Path projectDir)
    {
        this.config = config;
        this.projectDir = projectDir;
    }

    static PluginTestFixture create(final GradleTestConfig config, final Path projectDir)
    {
        return new PluginTestFixture(config, projectDir);
    }

    PluginTestFixture withArgs(final String... args)
    {
        this.arguments = args;
        return this;
    }

    public PluginTestFixture withReportFile(final Path relativeReportPath)
    {
        this.relativeReportPath = relativeReportPath;
        return this;
    }

    Result run()
    {
        final GradleRunner runner = createGradleRunner();
        final BuildResult buildResult = runner.build();
        return new Result(buildResult);
    }

    Result runExpectingFailure()
    {
        final GradleRunner runner = createGradleRunner();
        final BuildResult buildResult = runner.buildAndFail();
        return new Result(buildResult);
    }

    private GradleRunner createGradleRunner()
    {
        configureJacoco(projectDir);
        final List<String> allArgs = new ArrayList<>();
        allArgs.addAll(List.of(arguments));
        allArgs.addAll(List.of("--info", "--stacktrace", "--build-cache"));
        if (configurationCacheEnabled())
        {
            allArgs.add("--configuration-cache");
        }
        allArgs.addAll(List.of("--warning-mode", "all"));
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

    class Result
    {

        private final BuildResult buildResult;

        private Result(final BuildResult buildResult)
        {
            this.buildResult = buildResult;
        }

        void assertOutput(final Matcher<String> matcher)
        {
            assertThat(buildResult.getOutput(), matcher);
        }

        Result assertTraceOutcomeSuccessOrFromCache()
        {
            return assertOutcomeSuccessOrFromCache(":traceRequirements");
        }

        Result assertCollectOutcomeUpToDate()
        {
            return this.assertOutcome(":collectRequirements", TaskOutcome.UP_TO_DATE);
        }

        Result assertOutcome(final String taskPath, final TaskOutcome expectedOutcome)
        {
            assertEquals(expectedOutcome, buildResult.task(taskPath).getOutcome(),
                    "Outcome of task " + taskPath);
            return this;
        }

        Result assertCollectOutcomeSuccessOrFromCache()
        {
            return assertOutcomeSuccessOrFromCache(":collectRequirements");
        }

        Result assertOutcomeSuccessOrFromCache(final String taskPath)
        {
            assertThat(buildResult.task(taskPath).getOutcome(),
                    either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
            return this;
        }

        Result assertReportFileLines(final String... lines)
        {
            final Path reportFile = projectDir.resolve(relativeReportPath);
            TestUtil.assertFileContent(reportFile, lines);
            return this;
        }
    }
}
