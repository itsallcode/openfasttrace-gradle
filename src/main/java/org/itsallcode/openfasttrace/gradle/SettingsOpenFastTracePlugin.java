package org.itsallcode.openfasttrace.gradle;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/** Applies OpenFastTrace to each project and publishes immutable project paths to the root plugin. */
public class SettingsOpenFastTracePlugin implements Plugin<Settings>
{
    /** Creates the settings plugin. */
    public SettingsOpenFastTracePlugin()
    {
        super();
    }

    @Override
    public void apply(final Settings settings)
    {
        final Map<String, String> projectDirectories = new LinkedHashMap<>();
        settings.getGradle().settingsEvaluated(evaluatedSettings ->
                collectProjectDirectories(evaluatedSettings.getRootProject(), projectDirectories));
        settings.getGradle().beforeProject(project -> {
            if (project == project.getRootProject())
            {
                project.getExtensions().add(OftAggregationProjects.EXTENSION_NAME,
                        new OftAggregationProjects(projectDirectories));
            }
            project.getPluginManager().apply("org.itsallcode.openfasttrace");
        });
    }

    private static void collectProjectDirectories(
            final org.gradle.api.initialization.ProjectDescriptor project,
            final Map<String, String> directories)
    {
        directories.put(project.getPath(), project.getProjectDir().getAbsolutePath());
        project.getChildren().forEach(child -> collectProjectDirectories(child, directories));
    }

}

