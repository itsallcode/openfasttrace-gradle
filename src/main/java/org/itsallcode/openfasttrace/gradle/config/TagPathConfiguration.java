/**
 * openfasttrace-gradle - Gradle plugin for tracing requirements using OpenFastTrace
 * Copyright (C) 2017 It's all code <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.itsallcode.openfasttrace.gradle.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.itsallcode.openfasttrace.importer.tag.config.PathConfig;

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

    public Stream<Path> getPaths()
    {
        return tagConfigs.stream().map(TagConfig::getPaths).flatMap(List::stream);
    }

    @Override
    public String toString()
    {
        return "TagPathConfiguration [tagConfigs=" + tagConfigs + "]";
    }
}
