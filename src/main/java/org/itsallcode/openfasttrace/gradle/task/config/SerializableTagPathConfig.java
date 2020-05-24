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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.itsallcode.openfasttrace.api.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;

public class SerializableTagPathConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final List<SerializableTagConfig> tagConfigs;

    public SerializableTagPathConfig(TagPathConfiguration tagPathConfig)
    {
        tagConfigs = tagPathConfig.getPathConfig().stream().map(SerializableTagConfig::new)
                .collect(toList());
    }

    public Stream<Path> getPaths()
    {
        return tagConfigs.stream().map(SerializableTagConfig::getPaths).flatMap(List::stream);
    }

    public Stream<PathConfig> getPathConfig()
    {
        return tagConfigs.stream().map(SerializableTagConfig::convert);
    }

    @Override
    public String toString()
    {
        return "SerializableTagPathConfig [" + tagConfigs + "]";
    }
}
