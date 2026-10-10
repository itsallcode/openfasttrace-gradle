package org.itsallcode.openfasttrace.gradle.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.*;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.*;

/** Exports a project's tracing configuration for root-project aggregation. */
public abstract class OftExportConfigTask extends DefaultTask
{
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** Creates the task. */
    protected OftExportConfigTask()
    {
        super();
    }

    /**
     * Returns the encoded project-local configuration entries.
     *
     * @return encoded configuration entries
     */
    @Input
    public abstract ListProperty<String> getConfigEntries();

    /**
     * Returns the requirement and tag files that affect the export.
     *
     * @return requirement and tag source files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract ConfigurableFileCollection getSourceFiles();

    /**
     * Returns the resolved imported requirement archives.
     *
     * @return imported requirement archives
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract ConfigurableFileCollection getImportedArchives();

    /**
     * Returns the copied imported archives available to root tasks.
     *
     * @return output directory for copied imported archives
     */
    @OutputDirectory
    public abstract DirectoryProperty getImportedArchivesDirectory();

    /**
     * Returns the exported configuration manifest file.
     *
     * @return output manifest file
     */
    @OutputFile
    public abstract RegularFileProperty getManifestFile();

    /**
     * Writes a deterministic manifest and project-local copies of imported archives.
     *
     * @throws IOException
     *             if writing the manifest or copying imported archives fails
     */
    @TaskAction
    public void exportConfig() throws IOException
    {
        final Path importsDir = getImportedArchivesDirectory().get().getAsFile().toPath();
        if (Files.exists(importsDir))
        {
            try (var files = Files.walk(importsDir))
            {
                files.sorted(Comparator.reverseOrder()).forEach(OftExportConfigTask::delete);
            }
        }
        Files.createDirectories(importsDir);

        final List<String> entries = new ArrayList<>(getConfigEntries().get());
        final List<File> archives = getImportedArchives().getFiles().stream()
                .sorted(Comparator.comparing(File::getAbsolutePath)).toList();
        IntStream.range(0, archives.size()).forEach(index -> {
            final File archive = archives.get(index);
            final Path destination = importsDir.resolve(index + "-" + archive.getName());
            try
            {
                Files.copy(archive.toPath(), destination);
            }
            catch (final IOException e)
            {
                throw new IllegalStateException("Failed to copy imported requirement archive " + archive, e);
            }
            entries.add("R\t" + encode(destination.toAbsolutePath().toString()));
        });

        final Path manifest = getManifestFile().get().getAsFile().toPath();
        Files.createDirectories(manifest.getParent());
        Files.write(manifest, entries, StandardCharsets.UTF_8);
    }

    private static String encode(final String value)
    {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void delete(final Path path)
    {
        try
        {
            Files.delete(path);
        }
        catch (final IOException e)
        {
            throw new IllegalStateException("Failed to clean imported archive output " + path, e);
        }
    }
}
