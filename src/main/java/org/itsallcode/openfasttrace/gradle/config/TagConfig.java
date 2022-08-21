package org.itsallcode.openfasttrace.gradle.config;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

// Public fields are required for configuration via gradle
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TagConfig
{
    private final Project project;

    public FileCollection paths;
    public String coveredItemArtifactType;
    public String tagArtifactType;
    public String coveredItemNamePrefix;

    TagConfig(final Project project)
    {
        this.project = project;
    }

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