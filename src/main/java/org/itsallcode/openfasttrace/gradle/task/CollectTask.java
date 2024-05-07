package org.itsallcode.openfasttrace.gradle.task;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.itsallcode.openfasttrace.api.core.Newline;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.core.*;
import org.itsallcode.openfasttrace.gradle.task.config.SerializableTagPathConfig;

public class CollectTask extends DefaultTask
{
    // Possible 'this' escape before subclass is fully initialized
    @SuppressWarnings("this-escape")
    public final SetProperty<File> inputDirectories = getProject().getObjects()
            .setProperty(File.class);
    @SuppressWarnings("this-escape")
    public final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    // non-transient instance field of a serializable class declared with a
    // non-serializable type
    // We use only serializable types
    @SuppressWarnings({ "serial", "this-escape" })
    public final ListProperty<SerializableTagPathConfig> pathConfig = getProject().getObjects()
            .listProperty(SerializableTagPathConfig.class);

    @InputFiles
    public SetProperty<File> getInputDirectories()
    {
        return inputDirectories;
    }

    @OutputFile
    public RegularFileProperty getOutputFile()
    {
        return outputFile;
    }

    @Input
    public ListProperty<SerializableTagPathConfig> getPathConfig()
    {
        return pathConfig;
    }

    @TaskAction
    public void collectRequirements()
    {
        createReportOutputDir();

        final Oft oft = new OftRunner();
        final ImportSettings settings = getImportSettings();
        getLogger().info("Importing from {} locations {} and {} path configurations: {}",
                settings.getInputs().size(), settings.getInputs(), settings.getPathConfigs().size(),
                settings.getPathConfigs());
        final List<SpecificationItem> importedItems = oft.importItems(settings);
        final Path output = getOuputFileInternal().toPath();
        getLogger().info("Imported {} spec items, writing to {}", importedItems.size(), output);
        oft.exportToPath(importedItems, output, getExportSettings());
    }

    private static ExportSettings getExportSettings()
    {
        return ExportSettings.builder() //
                .outputFormat("specobject") //
                .newline(Newline.UNIX) //
                .build();
    }

    private ImportSettings getImportSettings()
    {
        return ImportSettings.builder() //
                .addInputs(getAllImportFiles()) //
                .pathConfigs(getPathConfigInternal()) //
                .build();
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> inputDirPaths = inputDirectories.get().stream() //
                .map(File::toPath);
        getLogger().info("Importing from {} input directories: {}", inputDirectories.get().size(),
                inputDirectories.get());
        final Stream<Path> inputTagPaths = pathConfig.get().stream()
                .flatMap(SerializableTagPathConfig::getPaths);
        getLogger().info("Importing from {} configured paths: {}", pathConfig.get().size(),
                pathConfig.get());
        return Stream.concat(inputDirPaths, inputTagPaths).collect(toList());
    }

    private List<PathConfig> getPathConfigInternal()
    {
        final List<PathConfig> paths = pathConfig.get().stream()
                .flatMap(SerializableTagPathConfig::getPathConfig).collect(toList());
        if (getLogger().isInfoEnabled())
        {
            getLogger().info("Got {} path configurations:\n{}", paths.size(),
                    paths.stream().map(CollectTask::formatPathConfig).collect(joining("\n")));
        }
        return paths;
    }

    private static String formatPathConfig(final PathConfig config)
    {
        return " - " + config.getDescription() + " (type " + config.getTagArtifactType()
                + "): covers '" + config.getCoveredItemArtifactType() + "', prefix: '"
                + config.getCoveredItemNamePrefix() + "'";
    }

    private void createReportOutputDir()
    {
        final File outputDir = getOuputFileInternal().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IllegalStateException("Error creating directory " + outputDir);
        }
    }

    private File getOuputFileInternal()
    {
        return outputFile.getAsFile().get();
    }
}
