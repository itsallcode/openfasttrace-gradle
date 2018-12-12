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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;
import org.itsallcode.openfasttrace.gradle.util.DeprecationUtil;
import org.itsallcode.openfasttrace.report.ReportVerbosity;

// Public fields are required for configuration via gradle
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TracingConfig
{
    private static final ReportVerbosity DEFAULT_REPORT_VERBOSITY = ReportVerbosity.FAILURE_DETAILS;
    private static final String DEFAULT_REPORT_FORMAT = "plain";

    private final Project project;
    public final Property<ReportVerbosity> reportVerbosity;
    public String reportFormat;
    public final ConfigurableFileCollection inputDirectories;
    public final RegularFileProperty reportFile;
    public List<Object> importedRequirements;
    public List<String> filteredTags;
    public List<String> filteredArtifactTypes;
    public boolean filterAcceptsItemsWithoutTag = true;

    public TracingConfig(Project project)
    {
        this.project = project;
        this.inputDirectories = project.files();
        this.reportFile = DeprecationUtil.createFileProperty(project);
        this.reportVerbosity = project.getObjects().property(ReportVerbosity.class);
        this.reportVerbosity.set(DEFAULT_REPORT_VERBOSITY);
        this.reportFormat = DEFAULT_REPORT_FORMAT;
        this.importedRequirements = new ArrayList<>();
        this.filteredTags = new ArrayList<>();
        this.filteredArtifactTypes = new ArrayList<>();
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

    public void setReportVerbosity(ReportVerbosity reportVerbosity)
    {
        this.reportVerbosity.set(reportVerbosity);
    }

    public TagPathConfiguration getTagPathConfig()
    {
        return ((ExtensionAware) this).getExtensions().getByType(TagPathConfiguration.class);
    }

    @Override
    public String toString()
    {
        return "TracingConfig [project=" + project + ", reportVerbosity=" + reportVerbosity
                + ", inputDirectories=" + inputDirectories + ", reportFile=" + reportFile
                + ", pathConfig=" + getTagPathConfig() + "]";
    }
}
