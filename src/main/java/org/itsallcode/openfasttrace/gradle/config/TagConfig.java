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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;

// Public fields are required for configuration via gradle
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TagConfig
{
    private final Project project;

    public FileTree paths;
    public String coveredItemArtifactType;
    public String tagArtifactType;
    public String coveredItemNamePrefix;

    public TagConfig(Project project)
    {
        this.project = project;
    }

    public PathConfig convert()
    {
        return PathConfig.builder() //
                .coveredItemArtifactType(coveredItemArtifactType)
                .coveredItemNamePrefix(getItemNamePrefix()) //
                .tagArtifactType(tagArtifactType).pathListMatcher(getPaths()) //
                .build();
    }

    public List<Path> getPaths()
    {
        return paths.getFiles().stream() //
                .map(File::toPath) //
                .collect(toList());
    }

    private String getItemNamePrefix()
    {
        if (coveredItemNamePrefix == null)
        {
            return project.getName() + ".";
        }
        return coveredItemNamePrefix;
    }

    @Override
    public String toString()
    {
        return "TagConfig [paths=" + paths + ", coveredItemArtifactType=" + coveredItemArtifactType
                + ", tagArtifactType=" + tagArtifactType + ", coveredItemNamePrefix="
                + coveredItemNamePrefix + "]";
    }
}