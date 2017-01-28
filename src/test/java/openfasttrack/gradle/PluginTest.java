/**
 * openfasttrack-gradle - Gradle plugin for tracing requirements using OpenFastTrack
 * Copyright (C) 2017 Hamster community <christoph at users.sourceforge.net>
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
package openfasttrack.gradle;

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

public class PluginTest
{
    private static final Path PROJECT_DIR = Paths.get("example-project").toAbsolutePath();
    private BuildResult buildResult;

    @Test
    public void testTracingTaskAddedToProject()
    {
        runBuild(PROJECT_DIR, "tasks");
        assertThat(buildResult.getOutput(), containsString(
                "traceRequirements - Trace requirements and generate tracing report"));
    }

    @Test
    public void testTraceExampleProject() throws IOException
    {
        runBuild(PROJECT_DIR, "traceRequirements");
        assertEquals(buildResult.task(":traceRequirements").getOutcome(), TaskOutcome.SUCCESS);
        final Path reportFile = PROJECT_DIR.resolve("build/reports/tracing.txt");
        final String reportContent = new String(Files.readAllBytes(reportFile),
                StandardCharsets.UTF_8);
        assertThat(reportContent, containsString("ok - 0 total"));
    }

    private void runBuild(Path projectDir, String... arguments)
    {
        buildResult = GradleRunner.create() //
                .withProjectDir(projectDir.toFile()) //
                .withPluginClasspath() //
                .withArguments(arguments) //
                .forwardOutput() //
                .build();
    }
}
