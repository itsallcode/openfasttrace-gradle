package org.itsallcode.openfasttrace.gradle.task;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.itsallcode.openfasttrace.api.core.Newline;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.core.*;
import org.itsallcode.openfasttrace.gradle.task.config.OftManifestReader;
import org.itsallcode.openfasttrace.gradle.task.config.SerializableTagPathConfig;

/** Gradle task that collects specification items into a specobject file. */
@CacheableTask
public class CollectTask extends DefaultTask
{
    /** Directories from which specification items are collected. */
    // Possible 'this' escape before subclass is fully initialized
    @SuppressWarnings("this-escape")
    public final SetProperty<File> inputDirectories = getProject().getObjects()
            .setProperty(File.class);
    /** Generated specobject file. */
    @SuppressWarnings("this-escape")
    public final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    /** Configured tag path settings. */
    @SuppressWarnings("this-escape")
    public final ListProperty<SerializableTagPathConfig> pathConfig = getProject().getObjects()
            .listProperty(SerializableTagPathConfig.class);
    @SuppressWarnings("this-escape")
    private final ConfigurableFileCollection manifestFiles = getProject().files();
    @SuppressWarnings("this-escape")
    private final ConfigurableFileCollection importedArchiveInputs = getProject().files();

    /** Creates the task. */
    public CollectTask()
    {
        super();
    }

    /**
     * Returns the directories from which specification items are collected.
     * 
     * @return the input directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public SetProperty<File> getInputDirectories()
    {
        return inputDirectories;
    }

    /**
     * Returns the generated specobject file.
     * 
     * @return the output file
     */
    @OutputFile
    public RegularFileProperty getOutputFile()
    {
        return outputFile;
    }

    /**
     * Returns the configured tag path settings.
     * 
     * @return the path settings
     */
    @Input
    public ListProperty<SerializableTagPathConfig> getPathConfig()
    {
        return pathConfig;
    }

    /**
     * Returns exported subproject manifests used to populate collection inputs.
     *
     * @return manifest files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public ConfigurableFileCollection getManifestFiles()
    {
        return manifestFiles;
    }

    /**
     * Returns copied requirement archives referenced by project manifests.
     *
     * @return copied requirement archive files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public ConfigurableFileCollection getImportedArchiveInputs()
    {
        return importedArchiveInputs;
    }

    /**
     * Collects specification items and writes the specobject file.
     *
     * @throws java.io.IOException
     *             if reading a manifest or writing the output file fails
     */
    @TaskAction
    public void collectRequirements() throws java.io.IOException
    {
        final OftManifestReader.Manifest manifest = OftManifestReader.read(manifestFiles.getFiles());
        getLogger().info("Read {} configuration manifests, {} input directories, {} tag configs",
                manifestFiles.getFiles().size(), manifest.inputDirectories().size(),
                manifest.pathConfigs().size());
        createReportOutputDir();

        final Oft oft = new OftRunner();
        final ImportSettings settings = getImportSettings(manifest);
        getLogger().info("Importing from {} locations {} and {} path configurations: {}",
                settings.getInputs().size(), settings.getInputs(), settings.getPathConfigs().size(),
                settings.getPathConfigs());
        final List<SpecificationItem> importedItems = oft.importItems(settings);
        final Path output = getOuputFileInternal().toPath();
        getLogger().info("Imported {} spec items, writing to {}", importedItems.size(), output);
        oft.exportToPath(importedItems, output, getExportSettings());
    }

    /**
     * Adds paths from a root-aggregated manifest.
     *
     * @param directories
     *            input directories to collect
     * @param paths
     *            tag path configurations to apply
     */
    public void addManifestInputs(final List<File> directories,
            final List<SerializableTagPathConfig> paths)
    {
        inputDirectories.addAll(directories);
        pathConfig.addAll(paths);
    }

    private static ExportSettings getExportSettings()
    {
        return ExportSettings.builder() //
                .outputFormat("specobject") //
                .newline(Newline.UNIX) //
                .build();
    }

    private ImportSettings getImportSettings(final OftManifestReader.Manifest manifest)
    {
        final List<Path> inputPaths = Stream.concat(inputDirectories.get().stream()
                .map(File::toPath), manifest.inputDirectories().stream().map(File::toPath)).toList();
        final List<Path> tagPaths = Stream.concat(pathConfig.get().stream()
                .flatMap(SerializableTagPathConfig::getPaths),
                manifest.pathConfigs().stream()
                        .flatMap(SerializableTagPathConfig::getPaths))
                .toList();
        final List<Path> importedPaths = manifest.importedArchives().stream().map(File::toPath)
                .toList();
        final List<PathConfig> configs = Stream.concat(pathConfig.get().stream()
                .flatMap(SerializableTagPathConfig::getPathConfig),
                manifest.pathConfigs().stream()
                        .flatMap(SerializableTagPathConfig::getPathConfig))
                .toList();
        getLogger().info("Importing from {} inputs, {} tag paths and {} imported archives",
                inputPaths.size(), tagPaths.size(), importedPaths.size());
        return ImportSettings.builder()
                .addInputs(Stream.concat(Stream.concat(inputPaths.stream(), tagPaths.stream()),
                        importedPaths.stream()).toList())
                .pathConfigs(configs)
                .build();
    }

    private List<PathConfig> getPathConfigInternal()
    {
        final List<PathConfig> paths = pathConfig.get().stream()
                .flatMap(SerializableTagPathConfig::getPathConfig).toList();
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
