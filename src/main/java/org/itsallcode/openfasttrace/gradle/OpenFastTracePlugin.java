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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskProvider;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;
import org.itsallcode.openfasttrace.gradle.config.TracingConfig;
import org.itsallcode.openfasttrace.gradle.task.CollectTask;
import org.itsallcode.openfasttrace.gradle.task.TraceTask;
import org.itsallcode.openfasttrace.gradle.task.config.SerializableTagPathConfig;
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
        final TaskProvider<CollectTask> collectTask = createCollectTask(rootProject);
        createTracingTask(rootProject, collectTask);
    }

    private TaskProvider<CollectTask> createCollectTask(Project rootProject)
    {
        return rootProject.getTasks().register("collectRequirements", CollectTask.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Collect requirements and generate specobject file");
            task.getInputDirectories().set(getAllInputDirectories(rootProject.getAllprojects()));
            task.getOutputFile()
                    .set(new File(rootProject.getBuildDir(), "reports/requirements.xml"));
            task.getPathConfig().set(getPathConfig(rootProject.getAllprojects()));
        });
    }

    private void createTracingTask(Project rootProject, TaskProvider<CollectTask> collectTask)
    {
        rootProject.getTasks().register("traceRequirements", TraceTask.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Trace requirements and generate tracing report");
            task.dependsOn(collectTask);
            final TracingConfig config = getConfig(rootProject);
            task.getRequirementsFile().set(collectTask.get().getOutputFile());
            if (config.getReportFile().isPresent())
            {
                task.getOutputFile().set(config.getReportFile());
            }
            else
            {
                final String extension = config.getReportFormat().get().equals("html") ? "html"
                        : "txt";
                task.getOutputFile()
                        .set(new File(rootProject.getBuildDir(), "reports/tracing." + extension));
            }
            task.getReportVerbosity().set(config.getReportVerbosity());
            task.getReportFormat().set(config.getReportFormat());
            task.getImportedRequirements()
                    .set(getImportedRequirements(rootProject.getAllprojects()));
            task.getFilteredArtifactTypes().set(config.getFilteredArtifactTypes());
            task.getFilteredTags().set(config.getFilteredTags());
            task.getFilterAcceptsItemsWithoutTag().set(config.getFilterAcceptsItemsWithoutTag());
        });
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
        getConfig(project).getImportedRequirements().get().forEach(dependency -> {
            LOG.info("Adding dependency {} with configuration {} to project {}", dependency,
                    configurationName, project);
            project.getDependencies().add(configurationName, dependency);
        });
        final Set<File> files = configuration.getFiles();
        LOG.info("Found {} dependency files: {}", files.size(), files);
        return files.stream();
    }

    private List<SerializableTagPathConfig> getPathConfig(Set<Project> allProjects)
    {
        return allProjects.stream() //
                .map(this::getTagPathConfig) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .collect(toList());
    }

    private Optional<SerializableTagPathConfig> getTagPathConfig(Project project)
    {
        final TagPathConfiguration tagPathConfig = getConfig(project).getTagPathConfig();
        if (tagPathConfig.getPathConfig().isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new SerializableTagPathConfig(tagPathConfig));
    }

    private TracingConfig getConfig(Project project)
    {
        return project.getExtensions().getByType(TracingConfig.class);
    }
}
