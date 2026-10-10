package org.itsallcode.openfasttrace.gradle.task.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Reads project configuration exported for root-project aggregation. */
public final class OftManifestReader
{
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private OftManifestReader()
    {
        // Utility class.
    }

    /**
     * Reads project-local inputs and tag paths from manifests.
     *
     * @param manifests
     *            manifest files to read
     * @return the combined project input, tag-path, and imported-archive configuration
     * @throws IOException
     *             if a manifest cannot be read
     */
    public static Manifest read(final Iterable<File> manifests) throws IOException
    {
        final List<File> inputDirectories = new ArrayList<>();
        final List<SerializableTagPathConfig> pathConfigs = new ArrayList<>();
        final List<File> importedArchives = new ArrayList<>();
        for (final File manifest : manifests)
        {
            if (!manifest.isFile())
            {
                continue;
            }
            for (final String line : Files.readAllLines(manifest.toPath(), StandardCharsets.UTF_8))
            {
                final String[] fields = line.split("\\t", -1);
                if (fields.length == 2 && "D".equals(fields[0]))
                {
                    inputDirectories.add(new File(decode(fields[1])));
                }
                else if (fields.length == 2 && "R".equals(fields[0]))
                {
                    importedArchives.add(new File(decode(fields[1])));
                }
                else if (fields.length == 5 && "T".equals(fields[0]))
                {
                    final SerializableTagConfig tag = new SerializableTagConfig(
                            Path.of(decode(fields[4])), decode(fields[3]), decode(fields[2]),
                            decode(fields[1]));
                    pathConfigs.add(new SerializableTagPathConfig(List.of(tag)));
                }
            }
        }
        return new Manifest(List.copyOf(inputDirectories), List.copyOf(pathConfigs),
                List.copyOf(importedArchives));
    }

    /**
     * Parsed manifest data.
     *
     * @param inputDirectories
     *            directories to import
     * @param pathConfigs
     *            tag path configurations
     * @param importedArchives
     *            requirement archives to import
     */
    public record Manifest(List<File> inputDirectories,
            List<SerializableTagPathConfig> pathConfigs, List<File> importedArchives)
    {
    }

    private static String decode(final String value)
    {
        return new String(DECODER.decode(value), StandardCharsets.UTF_8);
    }
}
