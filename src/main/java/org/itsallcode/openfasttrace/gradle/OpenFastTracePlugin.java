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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;
import org.itsallcode.openfasttrace.gradle.config.PathPatternConfig;
import org.itsallcode.openfasttrace.gradle.config.TracingConfig;
import org.itsallcode.openfasttrace.gradle.task.TraceTask;
import org.itsallcode.openfasttrace.importer.legacytag.PathConfig;
import org.slf4j.Logger;

public class OpenFastTracePlugin implements Plugin<Project>
{
    private static final Logger LOG = Logging.getLogger(OpenFastTracePlugin.class);
    private static final String TASK_GROUP_NAME = "trace";

    @Override
    public void apply(Project project)
    {
        LOG.info("Initializing OpenFastTrack plugin for project '{}'", project);
        project.allprojects(this::createConfigDsl);
        // createConfigDsl(project);
        createTasks(project);
    }

    private void createConfigDsl(Project project)
    {
        LOG.info("Setting up plugin configuration for project '{}'", project.getName());
        final NamedDomainObjectContainer<PathPatternConfig> pathConfig = project
                .container(PathPatternConfig.class);

        final TracingConfig tracingConfig = project.getExtensions().create("requirementTracing",
                TracingConfig.class, project, pathConfig);
        ((ExtensionAware) tracingConfig).getExtensions().add("pathConfig", pathConfig);
    }

    private void createTasks(Project project)
    {
        LOG.info("Creating tasks for project '{}'", project.getName());
        createTracingTask(project);
    }

    private void createTracingTask(Project project)
    {
        final TraceTask traceTask = createTask(project, "traceRequirements", TraceTask.class);
        traceTask.setGroup(TASK_GROUP_NAME);
        traceTask.setDescription("Trace requirements and generate tracing report");
        final TracingConfig config = getConfig(project);
        traceTask.inputDirectories.setFrom(config.inputDirectories);
        traceTask.outputFile.set(config.reportFile);
        traceTask.reportVerbosity.set(config.reportVerbosity);
        traceTask.pathConfig = () -> getPathConfigFromRootProject(project.getAllprojects());
    }

    private List<PathConfig> getPathConfigFromRootProject(Set<Project> allProjects)
    {
        return allProjects.stream() //
                .flatMap(this::getPathConfigFromProject) //
                .collect(toList());
    }

    private Stream<PathConfig> getPathConfigFromProject(Project project)
    {
        final TracingConfig tracingConfig = project.getExtensions().getByType(TracingConfig.class);
        return tracingConfig.getPathConfig();
    }

    private TracingConfig getConfig(Project project)
    {
        return project.getExtensions().getByType(TracingConfig.class);
    }

    private <T extends DefaultTask> T createTask(Project project, String taskName,
            Class<T> taskType)
    {
        final Map<String, Class<T>> taskConfig = singletonMap("type", taskType);
        final Task task = project.task(taskConfig, taskName);
        return taskType.cast(task);
    }
}
