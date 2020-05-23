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
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;
import org.itsallcode.openfasttrace.gradle.config.TracingConfig;
import org.itsallcode.openfasttrace.gradle.task.CollectTask;
import org.itsallcode.openfasttrace.gradle.task.TraceTask;
import org.slf4j.Logger;

public class OpenFastTracePlugin implements Plugin<Project>
{
    private static final Logger LOG = Logging.getLogger(OpenFastTracePlugin.class);
    private static final String TASK_GROUP_NAME = "trace";

    @Override
    public void apply(Project rootProject)
    {
        LOG.info("Initializing OpenFastTrack plugin for project '{}'", rootProject);
        rootProject.allprojects(this::createConfigDsl);
        createTasks(rootProject);
    }

    private void createConfigDsl(Project project)
    {
        LOG.info("Setting up plugin configuration for project '{}'", project.getName());

        final TracingConfig tracingConfig = project.getExtensions().create("requirementTracing",
                TracingConfig.class, project);
        ((ExtensionAware) tracingConfig).getExtensions().create("tags", TagPathConfiguration.class,
                project);
    }

    private void createTasks(Project rootProject)
    {
        LOG.info("Creating tasks for project '{}'", rootProject.getName());
        final CollectTask collectTask = createCollectTask(rootProject);
        createTracingTask(rootProject, collectTask);
    }

    private CollectTask createCollectTask(Project rootProject)
    {
        final TracingConfig tracingConfig = rootProject.getExtensions()
                .getByType(TracingConfig.class);
        final TaskProvider<CollectTask> collectTask = rootProject.getTasks()
                .register("collectRequirements", CollectTask.class, new Action<CollectTask>()
                {

                    @Override
                    public void execute(CollectTask task)
                    {
                        task.getInputDirectories()
                                .set(getAllInputDirectories(rootProject.getAllprojects()));
                        task.getOutputFile().set(
                                new File(rootProject.getBuildDir(), "reports/requirements.xml"));
                        task.getPathConfig().set(getPathConfig(rootProject.getAllprojects()));
                    }
                });

        collectTask.get().setGroup(TASK_GROUP_NAME);
        collectTask.get().setDescription("Collect requirements and generate specobject file");

        return collectTask;
    }

    private void createTracingTask(Project rootProject, CollectTask collectTask)
    {
        final TraceTask traceTask = createTask(rootProject, "traceRequirements", TraceTask.class);
        traceTask.setGroup(TASK_GROUP_NAME);
        traceTask.setDescription("Trace requirements and generate tracing report");
        traceTask.dependsOn(collectTask);
        final TracingConfig config = getConfig(rootProject);
        traceTask.requirementsFile.set(collectTask.outputFile);
        traceTask.getOutputFile().set(config.getReportFile());
        traceTask.getReportVerbosity().set(config.getReportVerbosity());
        traceTask.getReportFormat().set(config.getReportFormat());
        traceTask.importedRequirements = () -> getImportedRequirements(
                rootProject.getAllprojects());
        traceTask.filteredArtifactTypes = () -> getFilteredArtifactTypes(rootProject);
        traceTask.filteredTags = () -> getFilteredTags(rootProject);
        traceTask.filterAcceptsItemsWithoutTag = () -> config.filterAcceptsItemsWithoutTag;
    }

    private Set<String> getFilteredTags(Project rootProject)
    {
        return new HashSet<>(getConfig(rootProject).getFilteredTags());
    }

    private Set<String> getFilteredArtifactTypes(Project rootProject)
    {
        return new HashSet<>(getConfig(rootProject).getFilteredArtifactTypes());
    }

    private Set<File> getAllInputDirectories(Set<Project> allProjects)
    {
        return allProjects.stream() //
                .map(project -> getConfig(project).getInputDirectories().getFiles()) //
                .flatMap(Set::stream) //
                .collect(toSet());
    }

    private Set<File> getImportedRequirements(Set<Project> allProjects)
    {
        return allProjects.stream() //
                .flatMap(this::getImportedRequirements) //
                .collect(toSet());
    }

    private Stream<File> getImportedRequirements(Project project)
    {
        final String configurationName = "oftRequirementConfig";
        final Configuration configuration = project.getConfigurations().create(configurationName);
        getConfig(project).importedRequirements.forEach(dependency -> {
            LOG.info("Adding dependency {} with configuration {} to project {}", dependency,
                    configurationName, project);
            project.getDependencies().add(configurationName, dependency);
        });
        final Set<File> files = configuration.getFiles();
        LOG.info("Found {} dependency files: {}", files.size(), files);
        return files.stream();
    }

    private List<TagPathConfiguration> getPathConfig(Set<Project> allProjects)
    {
        return allProjects.stream() //
                .map(this::getTagPathConfig) //
                .collect(toList());
    }

    private TagPathConfiguration getTagPathConfig(Project project)
    {
        return getConfig(project).getTagPathConfig();
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
