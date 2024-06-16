package org.itsallcode.openfasttrace.gradle.config;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.itsallcode.openfasttrace.api.DetailsSectionDisplay;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;

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
    private final Property<DetailsSectionDisplay> detailsSectionDisplay;
    private final Property<Boolean> failBuild;

    public TracingConfig(final Project project)
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
        this.filterAcceptsItemsWithoutTag.set(true);
        this.detailsSectionDisplay = project.getObjects().property(DetailsSectionDisplay.class);
        this.detailsSectionDisplay.set(DetailsSectionDisplay.COLLAPSE);
        this.failBuild = project.getObjects().property(Boolean.class);
        this.failBuild.set(true);
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

    public Property<DetailsSectionDisplay> getDetailsSectionDisplay()
    {
        return detailsSectionDisplay;
    }

    public void setReportVerbosity(final String reportVerbosity)
    {
        setReportVerbosity(ReportVerbosity.valueOf(reportVerbosity));
    }

    public void setReportVerbosity(final ReportVerbosity reportVerbosity)
    {
        this.reportVerbosity.set(reportVerbosity);
    }

    public void setReportFormat(final String reportFormat)
    {
        this.reportFormat.set(reportFormat);
    }

    public void setInputDirectories(final ConfigurableFileCollection inputDirectories)
    {
        this.inputDirectories.setFrom(inputDirectories);
    }

    public void setReportFile(final RegularFileProperty reportFile)
    {
        this.reportFile.set(reportFile);
    }

    public void setImportedRequirements(final List<Object> importedRequirements)
    {
        this.importedRequirements.set(importedRequirements);
    }

    public void setFilteredTags(final List<String> filteredTags)
    {
        this.filteredTags.set(filteredTags);
    }

    public void setFilteredArtifactTypes(final List<String> filteredArtifactTypes)
    {
        this.filteredArtifactTypes.set(filteredArtifactTypes);
    }

    public void setFilterAcceptsItemsWithoutTag(final boolean filterAcceptsItemsWithoutTag)
    {
        this.filterAcceptsItemsWithoutTag.set(filterAcceptsItemsWithoutTag);
    }

    public void setDetailsSectionDisplay(final String detailsSectionDisplay)
    {
        this.detailsSectionDisplay.set(DetailsSectionDisplay.valueOf(detailsSectionDisplay));
    }

    public TagPathConfiguration getTagPathConfig()
    {
        return ((ExtensionAware) this).getExtensions().getByType(TagPathConfiguration.class);
    }

    public Property<Boolean> getFailBuild()
    {
        return failBuild;
    }

    public void setFailBuild(final boolean failBuild)
    {
        this.failBuild.set(failBuild);
    }

    @Override
    public String toString()
    {
        return "TracingConfig [reportVerbosity=" + reportVerbosity + ", inputDirectories="
                + inputDirectories + ", reportFile=" + reportFile + ", pathConfig="
                + getTagPathConfig() + ", failBuild=" + failBuild + "]";
    }
}
