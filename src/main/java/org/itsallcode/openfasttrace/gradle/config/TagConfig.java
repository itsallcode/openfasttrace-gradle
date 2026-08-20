package org.itsallcode.openfasttrace.gradle.config;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

/** Configuration for one set of requirement tags. */
// Public fields are required for configuration via gradle
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TagConfig
{
    private final Project project;

    /** Files containing the tags. */
    public FileCollection paths;
    /** Artifact type of the covered requirements. */
    public String coveredItemArtifactType;
    /** Artifact type of the tags. */
    public String tagArtifactType;
    /** Prefix used when matching covered requirement names. */
    public String coveredItemNamePrefix;

    TagConfig(final Project project)
    {
        this.project = project;
    }

    /**
     * Returns the name of the project to which this configuration belongs.
     *
     * @return the Gradle project name
     */
    public String getProjectName()
    {
        return project.getName();
    }

    @Override
    public String toString()
    {
        return "TagConfig [paths=" + paths + ", coveredItemArtifactType=" + coveredItemArtifactType
                + ", tagArtifactType=" + tagArtifactType + ", coveredItemNamePrefix="
                + coveredItemNamePrefix + "]";
    }
}