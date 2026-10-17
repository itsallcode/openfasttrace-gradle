package org.itsallcode.openfasttrace.gradle.task;

import java.io.File;
import java.nio.file.Path;

import org.gradle.api.file.ConfigurableFileCollection;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftBuilder;

class OftFactory
{
    static Oft createOft(final ConfigurableFileCollection pluginFiles)
    {
        final OftBuilder builder = Oft.builder();
        if (!pluginFiles.isEmpty())
        {
            builder.addPlugin("gradle-plugins", pluginFiles.getFiles().stream().map(File::toPath).toArray(Path[]::new));
        }
        return builder.build();
    }
}
