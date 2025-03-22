package org.itsallcode.openfasttrace.gradle;

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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class OpenFastTracePlugin implements Plugin<Project>
{
    private static final Logger LOG = Logging.getLogger(OpenFastTracePlugin.class);
    private static final String TASK_GROUP_NAME = "trace";

    @Override
    public void apply(final Project rootProject)
    {
        LOG.info("Initializing OpenFastTrack plugin for project '{}'", rootProject);
        rootProject.allprojects(OpenFastTracePlugin::createConfigDsl);
        System.setProperty( "oftProjectName", rootProject.getName() );
        createTasks(rootProject);
    }

    private static void createConfigDsl(final Project project)
    {
        LOG.info("Setting up plugin configuration for project '{}'", project.getName());

        final TracingConfig tracingConfig = project.getExtensions().create("requirementTracing",
                TracingConfig.class, project);
        ((ExtensionAware) tracingConfig).getExtensions().create("tags", TagPathConfiguration.class,
                project);
    }

    private static void createTasks(final Project rootProject)
    {
        LOG.info("Creating tasks for project '{}'", rootProject.getName());
        final TaskProvider<CollectTask> collectTask = createCollectTask(rootProject);
        createTracingTask(rootProject, collectTask);
    }

    private static TaskProvider<CollectTask> createCollectTask(final Project rootProject)
    {
        return rootProject.getTasks().register("collectRequirements", CollectTask.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Collect requirements and generate specobject file");
            task.getInputDirectories().set(getAllInputDirectories(rootProject.getAllprojects()));
            task.getOutputFile()
                    .set(new File(rootProject.getLayout().getBuildDirectory().getAsFile().get(),
                            "reports/requirements.xml"));
            task.getPathConfig().set(getPathConfig(rootProject.getAllprojects()));
        });
    }

    private static void createTracingTask(final Project rootProject,
            final TaskProvider<CollectTask> collectTask)
    {
        rootProject.getTasks().register("traceRequirements", TraceTask.class,
                task -> configureTask(rootProject, collectTask, task));
    }

    private static void configureTask(final Project rootProject,
            final TaskProvider<CollectTask> collectTask, final TraceTask task)
    {
        task.setGroup(TASK_GROUP_NAME);
        task.setDescription("Trace requirements and generate tracing report");
        task.dependsOn(collectTask);
        final TracingConfig config = getConfig(rootProject);
        task.getFailBuild().set(config.getFailBuild());
        task.getRequirementsFile().set(collectTask.get().getOutputFile());
        configureOutputs(rootProject, task, config);
        task.getReportVerbosity().set(config.getReportVerbosity());
        task.getReportFormat().set(config.getReportFormat());
        task.getImportedRequirements().set(getImportedRequirements(rootProject.getAllprojects()));
        task.getFilteredArtifactTypes().set(config.getFilteredArtifactTypes());
        task.getFilteredTags().set(config.getFilteredTags());
        task.getFilterAcceptsItemsWithoutTag().set(config.getFilterAcceptsItemsWithoutTag());
        task.getDetailsSectionDisplay().set(config.getDetailsSectionDisplay());
    }

    private static void configureOutputs( final Project rootProject,
                                          final TraceTask task,
                                          final TracingConfig config )
    {
        if( config.getReportFile().isPresent() )
        {
            task.getOutputFile().set( config.getReportFile() );
        }
        else
        {
            final String reporterFormat = config.getReportFormat().get();
            task.getOutputFile()
                    .set( new File( rootProject.getLayout().getBuildDirectory().getAsFile().get(),
                            toReporterFile( reporterFormat ) ) );
            final String resourceName = "openfasttrace-" + reporterFormat + ".zip";
            final URL resource = task.getClass().getClassLoader().getResource( resourceName );
            if( "ux".equals( reporterFormat ) )
            {
                task.getReportFile().set( "build/reports/openfasttrace/openfasttrace.html" );
            }
            if( resource != null )
                task.getAdditionalResources().add( resourceName );
        }
    }

    private static String toReporterFile( final String reporterFormat )
    {
        return "ux".equals( reporterFormat ) ? "reports/openfasttrace/resources/js/specitem_data.js"
                : "html".equals( reporterFormat ) ? "reports/tracing.html"
                : "reports/tracing.txt";

    }

    private static Set<File> getAllInputDirectories(final Set<Project> allProjects)
    {
        return allProjects.stream() //
                .map(project -> getConfig(project).getInputDirectories().getFiles()) //
                .flatMap(Set::stream) //
                .collect(toSet());
    }

    private static Set<File> getImportedRequirements(final Set<Project> allProjects)
    {
        return allProjects.stream() //
                .flatMap(OpenFastTracePlugin::getImportedRequirements) //
                .collect(toSet());
    }

    private static Stream<File> getImportedRequirements(final Project project)
    {
        final String CONFIG_NAME = "oftRequirementConfig";
        final Configuration configuration = project.getConfigurations().create(CONFIG_NAME);
        getConfig(project).getImportedRequirements().get().forEach(dependency -> {
            LOG.info("Adding dependency {} with configuration {} to project {}", dependency,
                    CONFIG_NAME, project);
            project.getDependencies().add(CONFIG_NAME, dependency);
        });
        final Set<File> files = configuration.getFiles();
        LOG.info("Found {} dependency files: {}", files.size(), files);
        return files.stream();
    }

    private static List<SerializableTagPathConfig> getPathConfig(final Set<Project> allProjects)
    {
        return allProjects.stream() //
                .map(OpenFastTracePlugin::getTagPathConfig) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .collect(toList());
    }

    private static Optional<SerializableTagPathConfig> getTagPathConfig(final Project project)
    {
        final TagPathConfiguration tagPathConfig = getConfig(project).getTagPathConfig();
        if (tagPathConfig.getPathConfig().isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(new SerializableTagPathConfig(tagPathConfig));
    }

    private static TracingConfig getConfig(final Project project)
    {
        return project.getExtensions().getByType(TracingConfig.class);
    }
}
