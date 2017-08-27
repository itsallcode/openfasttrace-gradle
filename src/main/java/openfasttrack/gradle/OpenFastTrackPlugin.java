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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logging;
import org.slf4j.Logger;

import openfasttrack.gradle.task.TraceTask;
import openfasttrack.report.ReportVerbosity;

public class OpenFastTrackPlugin implements Plugin<Project>
{
    private static final Logger LOG = Logging.getLogger(OpenFastTrackPlugin.class);
    private static final String TASK_GROUP = "trace";
    private static final String DEFAULT_REPORT_FILE = "reports/tracing.txt";
    private static final List<String> DEFAULT_DIRECTORIES = asList("src", "doc");

    @Override
    public void apply(Project project)
    {
        LOG.info("Initializing OpenFastTrack plugin for project '{}'", project);
        project.allprojects(this::createConfigDsl);
        project.allprojects(this::createTasks);
    }

    private void createConfigDsl(Project project)
    {
        LOG.info("Setting up plugin configuration for project '{}'", project.getName());
        project.getExtensions().create("requirementTracing", TracingConfig.class, project);
    }

    private void createTasks(Project project)
    {
        LOG.info("Creating tasks for project '{}'", project.getName());
        createTracingTask(project);
    }

    private void createTracingTask(Project project)
    {
        final TraceTask traceTask = createTask(project, "traceRequirements", TraceTask.class);
        traceTask.setGroup(TASK_GROUP);
        traceTask.setDescription("Trace requirements and generate tracing report");
        traceTask.inputDirectories.setFrom(getInputDirectories(project));
        traceTask.outputFile.setFrom(getReportFile(project));
        traceTask.reportVerbosity.set(ReportVerbosity.FAILURE_DETAILS);
    }

    private File getReportFile(Project project)
    {
        final TracingConfig config = getConfig(project);
        return config.reportFile.isPresent() ? config.reportFile.get()
                : new File(project.getBuildDir(), DEFAULT_REPORT_FILE);
    }

    private TracingConfig getConfig(Project project)
    {
        return project.getExtensions().getByType(TracingConfig.class);
    }

    private Set<File> getInputDirectories(Project project)
    {
        final TracingConfig config = getConfig(project);
        return !config.inputDirectories.isEmpty() ? config.inputDirectories.getFiles()
                : getDefaultInputDirectories(project);
    }

    private Set<File> getDefaultInputDirectories(Project project)
    {
        return DEFAULT_DIRECTORIES.stream() //
                .map(dir -> new File(project.getRootDir(), dir)) //
                .collect(toSet());
    }

    private <T extends DefaultTask> T createTask(Project project, String taskName,
            Class<T> taskType)
    {
        final Map<String, Class<T>> taskConfig = singletonMap("type", taskType);
        final Task task = project.task(taskConfig, taskName);
        return taskType.cast(task);
    }
}
