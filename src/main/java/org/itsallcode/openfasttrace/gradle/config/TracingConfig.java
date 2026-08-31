package org.itsallcode.openfasttrace.gradle.config;

import static java.util.stream.Collectors.joining;

import java.util.*;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.*;
import org.itsallcode.openfasttrace.api.ColorScheme;
import org.itsallcode.openfasttrace.api.DetailsSectionDisplay;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;

/** Configuration of requirement collection and tracing. */
public class TracingConfig
{
    private static final ReportVerbosity DEFAULT_REPORT_VERBOSITY = ReportVerbosity.FAILURE_DETAILS;
    private static final String DEFAULT_REPORT_FORMAT = "plain";

    private final Property<ReportVerbosity> reportVerbosity;
    private final Property<ColorScheme> reportColorScheme;
    private final Property<String> reportFormat;
    private final ConfigurableFileCollection inputDirectories;
    private final RegularFileProperty reportFile;
    private final ListProperty<Object> importedRequirements;
    private final SetProperty<String> filteredTags;
    private final SetProperty<String> filteredArtifactTypes;
    private final SetProperty<String> filterWantedStatuses;
    private final Property<Boolean> filterAcceptsItemsWithoutTag;
    private final Property<DetailsSectionDisplay> detailsSectionDisplay;
    private final Property<Boolean> failBuild;

    /**
     * Creates a tracing configuration with the plugin defaults.
     * 
     * @param project
     *            the Gradle project owning the configuration
     */
    public TracingConfig(final Project project)
    {
        this.inputDirectories = project.files();
        this.reportFile = project.getObjects().fileProperty();
        this.reportVerbosity = project.getObjects().property(ReportVerbosity.class);
        this.reportVerbosity.set(DEFAULT_REPORT_VERBOSITY);
        this.reportColorScheme = project.getObjects().property(ColorScheme.class);
        this.reportFormat = project.getObjects().property(String.class);
        this.reportFormat.set(DEFAULT_REPORT_FORMAT);
        this.importedRequirements = project.getObjects().listProperty(Object.class);
        this.filteredTags = project.getObjects().setProperty(String.class);
        this.filteredArtifactTypes = project.getObjects().setProperty(String.class);
        this.filterAcceptsItemsWithoutTag = project.getObjects().property(Boolean.class);
        this.filterWantedStatuses = project.getObjects().setProperty(String.class);
        this.filterAcceptsItemsWithoutTag.set(true);
        this.detailsSectionDisplay = project.getObjects().property(DetailsSectionDisplay.class);
        this.detailsSectionDisplay.set(DetailsSectionDisplay.COLLAPSE);
        this.failBuild = project.getObjects().property(Boolean.class);
        this.failBuild.set(true);
    }

    /**
     * Returns the report verbosity property.
     * 
     * @return the verbosity property
     */
    public Property<ReportVerbosity> getReportVerbosity()
    {
        return reportVerbosity;
    }

    /**
     * Returns the report color scheme.
     * 
     * @return the color scheme
     */
    public Property<ColorScheme> getReportColorScheme()
    {
        return reportColorScheme;
    }

    /**
     * Returns the report format property.
     * 
     * @return the format property
     */
    public Property<String> getReportFormat()
    {
        return reportFormat;
    }

    /**
     * Returns the directories containing requirements.
     * 
     * @return the input directories
     */
    public ConfigurableFileCollection getInputDirectories()
    {
        return inputDirectories;
    }

    /**
     * Returns the optional report output file.
     * 
     * @return the report file
     */
    public RegularFileProperty getReportFile()
    {
        return reportFile;
    }

    /**
     * Returns the external requirement dependencies.
     * 
     * @return the imported requirements
     */
    public ListProperty<Object> getImportedRequirements()
    {
        return importedRequirements;
    }

    /**
     * Returns the tags to include in tracing.
     * 
     * @return the tag filter
     */
    public SetProperty<String> getFilteredTags()
    {
        return filteredTags;
    }

    /**
     * Returns the artifact types to include in tracing.
     * 
     * @return the artifact type filter
     */
    public SetProperty<String> getFilteredArtifactTypes()
    {
        return filteredArtifactTypes;
    }

    /**
     * Returns whether untagged items are included.
     * 
     * @return the untagged item setting
     */
    public Property<Boolean> getFilterAcceptsItemsWithoutTag()
    {
        return filterAcceptsItemsWithoutTag;
    }

    /**
     * Returns the report details section display setting.
     * 
     * @return the display setting
     */
    public Property<DetailsSectionDisplay> getDetailsSectionDisplay()
    {
        return detailsSectionDisplay;
    }

    /**
     * Returns the statuses to include in tracing.
     * 
     * @return the status filter
     */
    public SetProperty<String> getFilterWantedStatuses()
    {
        return filterWantedStatuses;
    }

    /**
     * Sets the report verbosity by name.
     * 
     * @param reportVerbosity
     *            verbosity name
     */
    public void setReportVerbosity(final String reportVerbosity)
    {
        this.setReportVerbosity(convertVerbosity(reportVerbosity));
    }

    private static ReportVerbosity convertVerbosity(final String reportVerbosity)
    {
        try
        {
            return ReportVerbosity.valueOf(reportVerbosity.toUpperCase(Locale.ROOT));
        }
        catch (final IllegalArgumentException e)
        {
            final String validVerbosities = Arrays.stream(ReportVerbosity.values()).map(ReportVerbosity::name)
                    .collect(joining(", "));
            throw new IllegalArgumentException(
                    "Invalid verbosity '" + reportVerbosity + "'. Valid verbosities are: "
                            + validVerbosities,
                    e);
        }
    }

    /**
     * Sets the report verbosity.
     * 
     * @param reportVerbosity
     *            verbosity to use
     */
    public void setReportVerbosity(final ReportVerbosity reportVerbosity)
    {
        this.reportVerbosity.set(reportVerbosity);
    }

    /**
     * Sets the report color scheme.
     * 
     * @param reportColorScheme
     *            color scheme to use
     */
    public void setReportColorScheme(final String reportColorScheme)
    {
        this.setReportColorScheme(convertColorScheme(reportColorScheme));
    }

    private static ColorScheme convertColorScheme(final String reportColorScheme)
    {
        try
        {
            return ColorScheme.valueOf(reportColorScheme.toUpperCase(Locale.ROOT));
        }
        catch (final IllegalArgumentException e)
        {
            final String validColorSchemes = Arrays.stream(ColorScheme.values()).map(ColorScheme::name)
                    .collect(joining(", "));
            throw new IllegalArgumentException(
                    "Invalid color scheme '" + reportColorScheme + "'. Valid color schemes are: "
                            + validColorSchemes,
                    e);
        }
    }

    /**
     * Sets the report color scheme.
     * 
     * @param reportColorScheme
     *            color scheme to use
     */
    public void setReportColorScheme(final ColorScheme reportColorScheme)
    {
        this.reportColorScheme.set(reportColorScheme);
    }

    /**
     * Sets the report format.
     * 
     * @param reportFormat
     *            format to use
     */
    public void setReportFormat(final String reportFormat)
    {
        this.reportFormat.set(reportFormat);
    }

    /**
     * Sets the directories containing requirements.
     * 
     * @param inputDirectories
     *            directories to use
     */
    public void setInputDirectories(final ConfigurableFileCollection inputDirectories)
    {
        this.inputDirectories.setFrom(inputDirectories);
    }

    /**
     * Sets the report output file.
     * 
     * @param reportFile
     *            file to write
     */
    public void setReportFile(final RegularFileProperty reportFile)
    {
        this.reportFile.set(reportFile);
    }

    /**
     * Sets the external requirement dependencies.
     * 
     * @param importedRequirements
     *            dependencies to import
     */
    public void setImportedRequirements(final List<Object> importedRequirements)
    {
        this.importedRequirements.set(importedRequirements);
    }

    /**
     * Sets the tags to include in tracing.
     * 
     * @param filteredTags
     *            tags to include
     */
    public void setFilteredTags(final List<String> filteredTags)
    {
        this.filteredTags.set(filteredTags);
    }

    /**
     * Sets the artifact types to include in tracing.
     * 
     * @param filteredArtifactTypes
     *            artifact types to include
     */
    public void setFilteredArtifactTypes(final List<String> filteredArtifactTypes)
    {
        this.filteredArtifactTypes.set(filteredArtifactTypes);
    }

    /**
     * Sets whether untagged items are included.
     * 
     * @param filterAcceptsItemsWithoutTag
     *            whether to include untagged items
     */
    public void setFilterAcceptsItemsWithoutTag(final boolean filterAcceptsItemsWithoutTag)
    {
        this.filterAcceptsItemsWithoutTag.set(filterAcceptsItemsWithoutTag);
    }

    /**
     * Sets the report details section display setting by name.
     * 
     * @param detailsSectionDisplay
     *            display setting name
     */
    public void setDetailsSectionDisplay(final DetailsSectionDisplay detailsSectionDisplay)
    {
        this.detailsSectionDisplay.set(detailsSectionDisplay);
    }

    /**
     * Sets the report details section display setting by name.
     * 
     * @param detailsSectionDisplay
     *            display setting name
     */
    public void setDetailsSectionDisplay(final String detailsSectionDisplay)
    {
        this.setDetailsSectionDisplay(convertDetailsSelectionDisplay(detailsSectionDisplay));
    }

    private static DetailsSectionDisplay convertDetailsSelectionDisplay(final String detailsSectionDisplay)
    {
        try
        {
            return DetailsSectionDisplay.valueOf(detailsSectionDisplay.toUpperCase(Locale.ROOT));
        }
        catch (final IllegalArgumentException e)
        {
            final String validValues = Arrays.stream(DetailsSectionDisplay.values()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Invalid details section display '" + detailsSectionDisplay + "'. Valid values are: "
                            + validValues,
                    e);
        }
    }

    /**
     * Sets the statuses to include in tracing.
     * 
     * @param statuses
     *            statuses to include
     */
    public void setFilterWantedStatuses(final Set<String> statuses)
    {
        this.filterWantedStatuses.set(statuses);
    }

    /**
     * Returns the tag path configuration.
     * 
     * @return the tag path configuration
     */
    public TagPathConfiguration getTagPathConfig()
    {
        return ((ExtensionAware) this).getExtensions().getByType(TagPathConfiguration.class);
    }

    /**
     * Returns whether tracing defects fail the build.
     * 
     * @return the fail-build property
     */
    public Property<Boolean> getFailBuild()
    {
        return failBuild;
    }

    /**
     * Sets whether tracing defects fail the build.
     * 
     * @param failBuild
     *            whether defects should fail the build
     */
    public void setFailBuild(final boolean failBuild)
    {
        this.failBuild.set(failBuild);
    }

    @Override
    public String toString()
    {
        return "TracingConfig [reportVerbosity=" + reportVerbosity + ", reportColorScheme=" + reportColorScheme
                + ", inputDirectories=" + inputDirectories + ", reportFile=" + reportFile + ", pathConfig="
                + getTagPathConfig() + ", failBuild=" + failBuild + ", filteredArtifactTypes="
                + filteredArtifactTypes + ", filterWantedStatuses=" + filterWantedStatuses + "]";
    }
}
