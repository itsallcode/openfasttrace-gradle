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

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;

// Public fields are required for configuration via gradle
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TracingConfig
{
    private static final ReportVerbosity DEFAULT_REPORT_VERBOSITY = ReportVerbosity.FAILURE_DETAILS;
    private static final String DEFAULT_REPORT_FORMAT = "plain";

    private final Property<ReportVerbosity> reportVerbosity;
    private final Property<String> reportFormat;
    private final ConfigurableFileCollection inputDirectories;
    private final RegularFileProperty reportFile;
    private final ListProperty<Object> importedRequirements;
    private final ListProperty<String> filteredTags;
    private final ListProperty<String> filteredArtifactTypes;
    private final Property<Boolean> filterAcceptsItemsWithoutTag;

    public TracingConfig(Project project)
    {
        this.inputDirectories = project.files();
        this.reportFile = project.getObjects().fileProperty();
        this.reportVerbosity = project.getObjects().property(ReportVerbosity.class);
        this.reportVerbosity.set(DEFAULT_REPORT_VERBOSITY);
        this.reportFormat = project.getObjects().property(String.class);
        this.reportFormat.set(DEFAULT_REPORT_FORMAT);
        this.importedRequirements = project.getObjects().listProperty(Object.class);
        this.filteredTags = project.getObjects().listProperty(String.class);
        this.filteredArtifactTypes = project.getObjects().listProperty(String.class);
        this.filterAcceptsItemsWithoutTag = project.getObjects().property(Boolean.class);
        this.filterAcceptsItemsWithoutTag.set(false);
    }

    public Property<ReportVerbosity> getReportVerbosity()
    {
        return reportVerbosity;
    }

    public Property<String> getReportFormat()
    {
        return reportFormat;
    }

    public ConfigurableFileCollection getInputDirectories()
    {
        return inputDirectories;
    }

    public RegularFileProperty getReportFile()
    {
        return reportFile;
    }

    public ListProperty<Object> getImportedRequirements()
    {
        return importedRequirements;
    }

    public ListProperty<String> getFilteredTags()
    {
        return filteredTags;
    }

    public ListProperty<String> getFilteredArtifactTypes()
    {
        return filteredArtifactTypes;
    }

    public Property<Boolean> getFilterAcceptsItemsWithoutTag()
    {
        return filterAcceptsItemsWithoutTag;
    }

    public TagPathConfiguration getTagPathConfig()
    {
        return ((ExtensionAware) this).getExtensions().getByType(TagPathConfiguration.class);
    }

    @Override
    public String toString()
    {
        return "TracingConfig [reportVerbosity=" + reportVerbosity + ", inputDirectories="
                + inputDirectories + ", reportFile=" + reportFile + ", pathConfig="
                + getTagPathConfig() + "]";
    }
}
