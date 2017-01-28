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

import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

public class PluginTest
{
    private static final File PROJECT_DIR = new File("example-project");
    private BuildResult buildResult;

    @Test
    public void testGetTasks()
    {
        runBuild(PROJECT_DIR, "tasks");
        assertThat(buildResult.getOutput(), containsString("traceRequirements - blah"));
    }

    @Test
    public void testExampleProject()
    {
        runBuild(PROJECT_DIR, "traceRequirements");
        assertEquals(buildResult.task(":traceRequirements").getOutcome(), TaskOutcome.SUCCESS);
    }

    private void runBuild(File projectDir, String... arguments)
    {
        buildResult = GradleRunner.create() //
                .withProjectDir(projectDir.getAbsoluteFile()) //
                .withPluginClasspath() //
                .withArguments(arguments) //
                .forwardOutput() //
                .build();
    }
}
