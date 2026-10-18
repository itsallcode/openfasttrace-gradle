package org.itsallcode.openfasttrace.gradle.task;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.gradle.api.file.ConfigurableFileCollection;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OftFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OftFactory.class);

    private OftFactory()
    {
        // prevent instantiation
    }

    static Oft createOft(final ConfigurableFileCollection pluginFiles)
    {
        final OftBuilder builder = Oft.builder();
        if (!pluginFiles.isEmpty())
        {
            final Path[] plugins = pluginFiles.getFiles()
                    .stream()
                    .map(File::toPath)
                    .toArray(Path[]::new);
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Using {} plugin files: {}", plugins.length, Arrays.toString(plugins));
            }
            builder.addPlugin("gradle-plugins", plugins);
        }
        return builder.build();
    }
}
