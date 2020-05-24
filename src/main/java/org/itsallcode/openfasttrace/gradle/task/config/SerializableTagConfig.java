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
package org.itsallcode.openfasttrace.gradle.task.config;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.gradle.config.TagConfig;

public class SerializableTagConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final Set<File> paths;
    private final String coveredItemArtifactType;
    private final String tagArtifactType;
    private final String coveredItemNamePrefix;

    public SerializableTagConfig(TagConfig tagConfig)
    {
        paths = tagConfig.paths.getFiles();
        coveredItemArtifactType = tagConfig.coveredItemArtifactType;
        tagArtifactType = tagConfig.tagArtifactType;
        coveredItemNamePrefix = tagConfig.coveredItemNamePrefix != null
                ? tagConfig.coveredItemNamePrefix
                : tagConfig.getProjectName() + ".";
    }

    public List<Path> getPaths()
    {
        return paths.stream().map(File::toPath).collect(toList());
    }

    public String getCoveredItemArtifactType()
    {
        return coveredItemArtifactType;
    }

    public String getTagArtifactType()
    {
        return tagArtifactType;
    }

    public String getCoveredItemNamePrefix()
    {
        return coveredItemNamePrefix;
    }

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
