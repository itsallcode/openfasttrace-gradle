package org.itsallcode.openfasttrace.gradle;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertNotNull(arguments, "Arguments must be set before running the test fixture");
        configureJacoco(projectDir);
        final List<String> allArgs = new ArrayList<>();
        allArgs.addAll(List.of(arguments));
        allArgs.addAll(List.of("--info", "--stacktrace", "--build-cache"));
        allArgs.addAll(List.of("--configuration-cache", "--configuration-cache-problems=fail"));
        allArgs.addAll(List.of("--warning-mode", "fail"));
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

    private static void configureJacoco(final Path projectDir)
    {
        final Optional<String> testkitGradleConfig = TestUtil
                .readResource(PluginTestFixture.class,
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

        Result assertOutput(final Matcher<String> matcher)
        {
            assertThat(buildResult.getOutput(), matcher);
            return this;
        }

        Result assertTraceOutcomeSuccessOrFromCache()
        {
            return assertOutcomeSuccessOrFromCache(":traceRequirements");
        }

        Result assertTraceOutcomeSuccessFromCacheOrUpToDate()
        {
            return assertOutcome(":traceRequirements",
                    either(is(TaskOutcome.SUCCESS))
                            .or(is(TaskOutcome.FROM_CACHE))
                            .or(is(TaskOutcome.UP_TO_DATE)));
        }

        Result assertCollectOutcomeUpToDate()
        {
            return this.assertOutcome(":collectRequirements", TaskOutcome.UP_TO_DATE);
        }

        Result assertOutcome(final String taskPath, final TaskOutcome expectedOutcome)
        {
            return assertOutcome(taskPath, equalTo(expectedOutcome));
        }

        Result assertOutcome(final String taskPath, final Matcher<TaskOutcome> matcher)
        {
            final BuildTask task = buildResult.task(taskPath);
            assertNotNull(task, "Task '" + taskPath + "' was not executed");
            assertThat("Outcome of task " + taskPath, task.getOutcome(), matcher);
            return this;
        }

        Result assertCollectOutcomeSuccessOrFromCache()
        {
            return assertOutcomeSuccessOrFromCache(":collectRequirements");
        }

        Result assertOutcomeSuccessOrFromCache(final String taskPath)
        {
            return assertOutcome(taskPath,
                    either(is(TaskOutcome.SUCCESS)).or(is(TaskOutcome.FROM_CACHE)));
        }

        Result assertReportFileLines(final String... lines)
        {
            assertNotNull(relativeReportPath,
                    "Report file path must be set before asserting report file lines");
            final Path reportFile = projectDir.resolve(relativeReportPath);
            TestUtil.assertFileContent(reportFile, lines);
            return this;
        }
    }
}
