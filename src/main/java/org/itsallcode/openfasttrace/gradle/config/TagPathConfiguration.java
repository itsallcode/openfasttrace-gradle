package org.itsallcode.openfasttrace.gradle.config;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;

/** Configuration of tag paths for a project. */
public class TagPathConfiguration
{
    private final Project project;
    private final List<TagConfig> tagConfigs = new ArrayList<>();

    /**
     * Creates an empty tag path configuration.
     *
     * @param project
     *            the Gradle project owning the configuration
     */
    public TagPathConfiguration(final Project project)
    {
        this.project = project;
    }

    /**
     * Adds a tag configuration.
     *
     * @param action
     *            action used to configure the tag
     */
    public void tag(final Action<TagConfig> action)
    {
        final TagConfig tagConfig = new TagConfig(project);
        action.execute(tagConfig);
        tagConfigs.add(tagConfig);
    }

    /**
     * Returns the configured tag paths.
     *
     * @return the tag configurations
     */
    public List<TagConfig> getPathConfig()
    {
        return tagConfigs;
    }

    @Override
    public String toString()
    {
        return "TagPathConfiguration [tagConfigs=" + tagConfigs + "]";
    }
}
