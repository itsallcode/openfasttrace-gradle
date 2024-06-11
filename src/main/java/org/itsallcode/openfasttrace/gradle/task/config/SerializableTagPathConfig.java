package org.itsallcode.openfasttrace.gradle.task.config;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;

public class SerializableTagPathConfig implements Serializable
{
    private static final long serialVersionUID = 1L;
    // non-transient instance field of a serializable class declared with a
    // non-serializable type (Java 21)
    // We use only serializable types
    @SuppressWarnings("serial")
    private final List<SerializableTagConfig> tagConfigs;

    public SerializableTagPathConfig(final TagPathConfiguration tagPathConfig)
    {
        tagConfigs = tagPathConfig.getPathConfig().stream().map(SerializableTagConfig::new)
                .collect(toList());
    }

    public Stream<Path> getPaths()
    {
        return tagConfigs.stream().map(SerializableTagConfig::getPaths).flatMap(List::stream);
    }

    public Stream<PathConfig> getPathConfig()
    {
        return tagConfigs.stream().map(SerializableTagConfig::convert);
    }

    @Override
    public String toString()
    {
        return "SerializableTagPathConfig [tagConfigs=" + tagConfigs + "]";
    }
}
