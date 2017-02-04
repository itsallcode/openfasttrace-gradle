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
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logging;
import org.slf4j.Logger;

import openfasttrack.gradle.task.TraceTask;
import openfasttrack.report.ReportVerbosity;

public class OpenFastTrackPlugin implements Plugin<Project>
{
    private static final Logger LOG = Logging.getLogger(OpenFastTrackPlugin.class);
    private static final String TASK_GROUP = "trace";
    private static final List<String> DEFAULT_DIRECTORIES = asList("src", "doc");
    private Project project;
    private TracingConfig config;

    @Override
    public void apply(Project project)
    {
        LOG.info("Initialize OpenFastTrack plugin...");
        this.project = project;
        this.config = createConfigDsl();
        project.afterEvaluate((p) -> createTasks());
    }

    private TracingConfig createConfigDsl()
    {
        LOG.debug("Setup serverless config DSL");
        return project.getExtensions().create("requirementTracing", TracingConfig.class);
    }

    private void createTasks()
    {
        LOG.debug("Creating tasks");
        createTracingTask();
    }

    private void createTracingTask()
    {
        final TraceTask traceTask = createTask("traceRequirements", TraceTask.class);
        traceTask.setGroup(TASK_GROUP);
        traceTask.setDescription("Trace requirements and generate tracing report");
        traceTask.inputDirectories = getInputDirectories();
        traceTask.outputFile = getReportFile();
        traceTask.reportVerbosity = ReportVerbosity.FAILURE_DETAILS;
    }

    private File getReportFile()
    {
        return config.reportFile != null ? config.reportFile
                : new File(project.getBuildDir(), "reports/tracing.txt");
    }

    private List<File> getInputDirectories()
    {
        return config.inputDirectories != null ? config.inputDirectories
                : getDefaultInputDirectories();
    }

    private List<File> getDefaultInputDirectories()
    {
        return DEFAULT_DIRECTORIES.stream() //
                .map(dir -> new File(project.getRootDir(), dir)) //
                .collect(toList());
    }

    private <T extends DefaultTask> T createTask(String taskName, Class<T> taskType)
    {
        return taskType.cast(project.task(singletonMap("type", taskType), taskName));
    }
}
