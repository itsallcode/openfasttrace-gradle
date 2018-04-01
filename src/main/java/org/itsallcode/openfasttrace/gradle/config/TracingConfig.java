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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.itsallcode.openfasttrace.importer.legacytag.PathConfig;
import org.itsallcode.openfasttrace.report.ReportVerbosity;

public class TracingConfig
{
    private static final String DEFAULT_REPORT_FILE = "reports/tracing.txt";
    private static final List<String> DEFAULT_DIRECTORIES = asList("src", "doc");

    private final Project project;
    public final Property<ReportVerbosity> reportVerbosity;
    public final ConfigurableFileCollection inputDirectories;
    public final RegularFileProperty reportFile;
    private final NamedDomainObjectContainer<PathPatternConfig> pathConfig;

    public TracingConfig(Project project, NamedDomainObjectContainer<PathPatternConfig> pathConfig)
    {
        this.project = project;
        this.pathConfig = pathConfig;
        this.inputDirectories = project.files(getDefaultInputDirectories(project));
        this.reportFile = project.getLayout().fileProperty();
        this.reportFile.set(new File(project.getBuildDir(), DEFAULT_REPORT_FILE));
        this.reportVerbosity = project.getObjects().property(ReportVerbosity.class);
        this.reportVerbosity.set(ReportVerbosity.FAILURE_DETAILS);
    }

    public void setInputDirectories(FileCollection inputDirs)
    {
        this.inputDirectories.setFrom(inputDirs);
    }

    public void setReportFile(File reportFile)
    {
        this.reportFile.set(reportFile);
    }

    public RegularFileProperty getReportFile()
    {
        return reportFile;
    }

    private Set<File> getDefaultInputDirectories(Project project)
    {
        return DEFAULT_DIRECTORIES.stream() //
                .map(dir -> new File(project.getRootDir(), dir)) //
                .collect(toSet());
    }

    public void setReportVerbosity(ReportVerbosity reportVerbosity)
    {
        this.reportVerbosity.set(reportVerbosity);
    }

    public Stream<PathConfig> getPathConfig()
    {
        return pathConfig.stream().map(c -> c.convert(project)).peek(System.out::println);
    }

    @Override
    public String toString()
    {
        return "TracingConfig [project=" + project + ", reportVerbosity=" + reportVerbosity
                + ", inputDirectories=" + inputDirectories + ", reportFile=" + reportFile
                + ", pathConfig=" + pathConfig + "]";
    }

    public static class PathPatternConfig
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

        private PathConfig convert(Project project)
        {
            return new PathConfig(getPattern(project), coveredItemArtifactType, getPrefix(project),
                    tagArtifactType);
        }

        private String getPattern(Project project)
        {
            final String relativeProjectPath = getRelativeProjectPath(project);
            final String pattern = getPatternWithoutPrefix();
            return getPrefix() + relativeProjectPath
                    + (pattern.startsWith("/") || relativeProjectPath.isEmpty() ? "" : "/")
                    + pattern;
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

        private String getPrefix(Project project)
        {
            if (coveredItemNamePrefix != null && !coveredItemNamePrefix.isEmpty())
            {
                return coveredItemNamePrefix;
            }
            else
            {
                return project.getName();
            }
        }
    }
}
