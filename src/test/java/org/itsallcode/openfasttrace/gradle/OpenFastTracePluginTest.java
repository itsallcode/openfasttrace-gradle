/**
 * openfasttrace-gradle - Gradle plugin for tracing requirements using OpenFastTrace
 * Copyright (C) 2017 It's all code <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.itsallcode.openfasttrace.gradle;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

public class OpenFastTracePluginTest
{
    private static final Path EXAMPLES_DIR = Paths.get("example-projects").toAbsolutePath();
    private static final Path PROJECT_DEFAULT_CONFIG_DIR = EXAMPLES_DIR.resolve("default-config");
    private static final Path PROJECT_CUSTOM_CONFIG_DIR = EXAMPLES_DIR.resolve("custom-config");
    private static final Path MULTI_PROJECT_DIR = EXAMPLES_DIR.resolve("multi-project");
    private BuildResult buildResult;

    @Test
    public void testTracingTaskAddedToProject()
    {
        runBuild(PROJECT_DEFAULT_CONFIG_DIR, "tasks", "--stacktrace");
        assertThat(buildResult.getOutput(), containsString(
                "traceRequirements - Trace requirements and generate tracing report"));
    }

    @Test
    public void testTraceExampleProjectWithDefaultConfig() throws IOException
    {
        runBuild(PROJECT_DEFAULT_CONFIG_DIR, "traceRequirements", "--stacktrace", "--info");
        assertEquals(buildResult.task(":traceRequirements").getOutcome(), TaskOutcome.SUCCESS);
        assertFileContent(PROJECT_DEFAULT_CONFIG_DIR.resolve("build/reports/tracing.txt"),
                "ok - 0 total");
    }

    @Test
    public void testTraceExampleProjectWithCustomConfig() throws IOException
    {
        runBuild(PROJECT_CUSTOM_CONFIG_DIR, "traceRequirements", "--info", "--stacktrace");
        assertEquals(buildResult.task(":traceRequirements").getOutcome(), TaskOutcome.SUCCESS);
        assertFileContent(PROJECT_CUSTOM_CONFIG_DIR.resolve("build/custom-report.txt"),
                "not ok - 0/1>0>0/0 - dsn~exampleB~1 (impl, -utest)", "not ok - 2 total, 2 defect");
    }

    @Test
    public void testTraceMultiProject() throws IOException
    {
        runBuild(MULTI_PROJECT_DIR, "traceRequirements", "--info", "--stacktrace");
        assertEquals(buildResult.task(":traceRequirements").getOutcome(), TaskOutcome.SUCCESS);
        assertFileContent(MULTI_PROJECT_DIR.resolve("build/custom-report.txt"), "ok - 6 total");
    }

    private void assertFileContent(Path file, String... lines) throws IOException
    {
        final String fileContent = fileContent(file);
        for (final String line : lines)
        {
            assertThat(fileContent, containsString(line));
        }
    }

    private String fileContent(Path file) throws IOException
    {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private void runBuild(Path projectDir, String... arguments)
    {
        deleteBuildDir(projectDir);
        configureJacoco(projectDir);
        buildResult = GradleRunner.create() //
                .withProjectDir(projectDir.toFile()) //
                .withPluginClasspath() //
                .withArguments(arguments) //
                .forwardOutput() //
                .build();
    }

    private void configureJacoco(Path projectDir)
    {
        final String testkitGradleConfig = TestUtil.readResource(this.getClass(),
                "/testkit-gradle.properties");
        TestUtil.writeFile(projectDir.resolve("gradle.properties"), testkitGradleConfig);
    }

    private void deleteBuildDir(Path projectDir)
    {
        TestUtil.deleteRecursive(projectDir.resolve("build"));
    }
}
