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

import org.gradle.api.Project;
import org.itsallcode.openfasttrace.importer.legacytag.PathConfig;

public class PathPatternConfig
{
    private static final String GLOB_PREFIX = "glob:";
    private static final String REGEX_PREFIX = "regex:";
    private final String pathPattern;
    public String coveredItemArtifactType;
    public String tagArtifactType;
    public String coveredItemNamePrefix;

    public PathPatternConfig(String pathPattern)
    {
        this.pathPattern = pathPattern;
    }

    public String getPathPattern()
    {
        return pathPattern;
    }

    public String getName()
    {
        return getPathPattern();
    }

    PathConfig convert(Project project)
    {
        return new PathConfig(getPattern(project), coveredItemArtifactType,
                getItemNamePrefix(project), tagArtifactType);
    }

    private String getPattern(Project project)
    {
        final String relativeProjectPath = getRelativeProjectPath(project);
        final String pattern = getPatternWithoutPrefix();
        return getPrefix() + relativeProjectPath
                + (pattern.startsWith("/") || relativeProjectPath.isEmpty() ? "" : "/") + pattern;
    }

    private String getPatternWithoutPrefix()
    {
        if (pathPattern.startsWith(GLOB_PREFIX))
        {
            return pathPattern.substring(GLOB_PREFIX.length());
        }
        else if (pathPattern.startsWith(REGEX_PREFIX))
        {
            return pathPattern.substring(REGEX_PREFIX.length());
        }
        return pathPattern;
    }

    private String getPrefix()
    {
        if (pathPattern.startsWith(GLOB_PREFIX))
        {
            return GLOB_PREFIX;
        }
        else if (pathPattern.startsWith(REGEX_PREFIX))
        {
            return REGEX_PREFIX;
        }
        return "";
    }

    private String getRelativeProjectPath(Project project)
    {
        final Path rootDir = project.getRootDir().toPath();
        final Path projectDir = project.getProjectDir().toPath();
        final String relativeProjectPath = rootDir.relativize(projectDir).toString();
        return relativeProjectPath;
    }

    private String getItemNamePrefix(Project project)
    {
        if (coveredItemNamePrefix != null && !coveredItemNamePrefix.isEmpty())
        {
            return coveredItemNamePrefix;
        }
        else
        {
            return project.getName() + ".";
        }
    }
}