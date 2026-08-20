package org.itsallcode.openfasttrace.gradle.task.config;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.gradle.config.TagConfig;

/** Serializable form of a tag configuration used by a Gradle task. */
public class SerializableTagConfig implements Serializable
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;
    // non-transient instance field of a serializable class declared with a
    // non-serializable type (Java 21)
    // We use only serializable types
    /** Configured tag paths. */
    @SuppressWarnings("serial")
    private final Set<File> paths;
    /** Covered requirement artifact type. */
    private final String coveredItemArtifactType;
    /** Tag artifact type. */
    private final String tagArtifactType;
    /** Covered requirement name prefix. */
    private final String coveredItemNamePrefix;

    /**
     * Creates a serializable copy of a tag configuration.
     *
     * @param tagConfig
     *            configuration to copy
     */
    public SerializableTagConfig(final TagConfig tagConfig)
    {
        paths = tagConfig.paths.getFiles();
        coveredItemArtifactType = tagConfig.coveredItemArtifactType;
        tagArtifactType = tagConfig.tagArtifactType;
        coveredItemNamePrefix = tagConfig.coveredItemNamePrefix != null
                ? tagConfig.coveredItemNamePrefix
                : (tagConfig.getProjectName() + ".");
    }

    /**
     * Returns the configured tag paths.
     *
     * @return the configured paths
     */
    public List<Path> getPaths()
    {
        return paths.stream().map(File::toPath).toList();
    }

    /**
     * Returns the covered requirement artifact type.
     *
     * @return the artifact type
     */
    public String getCoveredItemArtifactType()
    {
        return coveredItemArtifactType;
    }

    /**
     * Returns the tag artifact type.
     *
     * @return the artifact type
     */
    public String getTagArtifactType()
    {
        return tagArtifactType;
    }

    /**
     * Returns the covered requirement name prefix.
     *
     * @return the name prefix
     */
    public String getCoveredItemNamePrefix()
    {
        return coveredItemNamePrefix;
    }

    /**
     * Converts this value to an OpenFastTrace path configuration.
     *
     * @return the OpenFastTrace configuration
     */
    public PathConfig convert()
    {
        return PathConfig.builder() //
                .coveredItemArtifactType(coveredItemArtifactType)
                .coveredItemNamePrefix(coveredItemNamePrefix) //
                .tagArtifactType(tagArtifactType) //
                .pathListMatcher(getPaths()) //
                .build();
    }

    @Override
    public String toString()
    {
        return "SerializableTagConfig [paths=" + paths + ", coveredItemArtifactType="
                + coveredItemArtifactType + ", tagArtifactType=" + tagArtifactType
                + ", coveredItemNamePrefix=" + coveredItemNamePrefix + "]";
    }
}
