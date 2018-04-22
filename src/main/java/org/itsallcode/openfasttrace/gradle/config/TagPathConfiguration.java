package org.itsallcode.openfasttrace.gradle.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.itsallcode.openfasttrace.importer.legacytag.config.PathConfig;

import groovy.lang.Closure;

public class TagPathConfiguration
{
    private final Project project;
    private final List<TagConfig> tagConfigs = new ArrayList<>();

    public TagPathConfiguration(Project project)
    {
        this.project = project;
    }

    public void tag(Closure<?> closure)
    {
        final TagConfig tag = new TagConfig(project);
        project.configure(tag, closure);
        tagConfigs.add(tag);
    }

    public Stream<PathConfig> getPathConfig()
    {
        return tagConfigs.stream().map(TagConfig::convert);
    }

    @Override
    public String toString()
    {
        return "TagPathConfiguration [tagConfigs=" + tagConfigs + "]";
    }
}
