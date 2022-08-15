package org.itsallcode.openfasttrace.gradle.config;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;

public class TagPathConfiguration
{
    private final Project project;
    private final List<TagConfig> tagConfigs = new ArrayList<>();

    public TagPathConfiguration(Project project)
    {
        this.project = project;
    }

    public void tag(Action<TagConfig> action)
    {
        final TagConfig tagConfig = new TagConfig(project);
        action.execute(tagConfig);
        tagConfigs.add(tagConfig);
    }

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
