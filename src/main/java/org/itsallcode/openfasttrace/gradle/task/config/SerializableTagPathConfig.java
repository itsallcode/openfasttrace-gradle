package org.itsallcode.openfasttrace.gradle.task.config;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;

/** Serializable form of a tag path configuration used by a Gradle task. */
public class SerializableTagPathConfig implements Serializable
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;
    // non-transient instance field of a serializable class declared with a
    // non-serializable type (Java 21)
    // We use only serializable types
    /** Configured tag paths. */
    @SuppressWarnings("serial")
    private final List<SerializableTagConfig> tagConfigs;

    /**
     * Creates a serializable copy of a tag path configuration.
     *
     * @param tagPathConfig
     *            configuration to copy
     */
    public SerializableTagPathConfig(final TagPathConfiguration tagPathConfig)
    {
        tagConfigs = tagPathConfig.getPathConfig().stream().map(SerializableTagConfig::new)
                .toList();
    }

    /**
     * Returns all paths in this configuration.
     *
     * @return all configured paths
     */
    public Stream<Path> getPaths()
    {
        return tagConfigs.stream().map(SerializableTagConfig::getPaths).flatMap(List::stream);
    }

    /**
     * Returns the OpenFastTrace path configurations.
     *
     * @return the path configurations
     */
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
