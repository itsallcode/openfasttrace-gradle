package org.itsallcode.openfasttrace.gradle.task;

import static java.util.Collections.emptySet;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.itsallcode.openfasttrace.api.*;
import org.itsallcode.openfasttrace.api.ReportSettings.Builder;
import org.itsallcode.openfasttrace.api.core.*;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftRunner;

/** Gradle task that traces requirements and writes a report. */
@SuppressWarnings("this-escape")
@CacheableTask
public class TraceTask extends DefaultTask
{
    private static final ColorScheme DEFAULT_COLOR_SCHEME = ColorScheme.BLACK_AND_WHITE;

    private final RegularFileProperty requirementsFile = getProject().getObjects().fileProperty();
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);
    private final Property<ColorScheme> reportColorScheme = getProject().getObjects()
            .property(ColorScheme.class);
    private final Property<String> reportFormat = getProject().getObjects().property(String.class);
    private final Property<DetailsSectionDisplay> detailsSectionDisplay = getProject().getObjects()
            .property(DetailsSectionDisplay.class);
    private final ConfigurableFileCollection importedRequirements = getProject().files();
    private final SetProperty<String> filteredArtifactTypes = getProject().getObjects()
            .setProperty(String.class);
    private final SetProperty<String> filteredTags = getProject().getObjects()
            .setProperty(String.class);
    private final Property<Boolean> filterAcceptsItemsWithoutTag = getProject().getObjects()
            .property(Boolean.class);
    private final Property<Boolean> failBuild = getProject().getObjects().property(Boolean.class);
    private final SetProperty<ItemStatus> filterWantedStatuses = getProject().getObjects()
            .setProperty(ItemStatus.class);

    /** Creates the task. */
    public TraceTask()
    {
        super();
    }

    /**
     * Returns the collected requirements file.
     * 
     * @return the requirements file
     */
    @InputFile
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public RegularFileProperty getRequirementsFile()
    {
        return requirementsFile;
    }

    /**
     * Returns the generated tracing report.
     * 
     * @return the report file
     */
    @OutputFile
    public RegularFileProperty getOutputFile()
    {
        return outputFile;
    }

    /**
     * Returns the report verbosity property.
     * 
     * @return the verbosity property
     */
    @Input
    public Property<ReportVerbosity> getReportVerbosity()
    {
        return reportVerbosity;
    }

    /**
     * Get the report color scheme property.
     * 
     * @return the color scheme property
     */
    @Input
    @Optional
    public Property<ColorScheme> getReportColorScheme()
    {
        return reportColorScheme;
    }

    /**
     * Returns the report format property.
     * 
     * @return the format property
     */
    @Input
    public Property<String> getReportFormat()
    {
        return reportFormat;
    }

    /**
     * Returns the imported requirements property.
     * 
     * @return the imported requirements property
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public ConfigurableFileCollection getImportedRequirements()
    {
        return importedRequirements;
    }

    /**
     * Returns the artifact type filter.
     * 
     * @return the artifact type filter
     */
    @Input
    @Optional
    public SetProperty<String> getFilteredArtifactTypes()
    {
        return filteredArtifactTypes;
    }

    /**
     * Returns the tag filter.
     * 
     * @return the tag filter
     */
    @Input
    public SetProperty<String> getFilteredTags()
    {
        return filteredTags;
    }

    /**
     * Returns whether items without tags are accepted.
     * 
     * @return the untagged item setting
     */
    @Input
    public Property<Boolean> getFilterAcceptsItemsWithoutTag()
    {
        return filterAcceptsItemsWithoutTag;
    }

    /**
     * Returns the report details section display setting.
     * 
     * @return the display setting
     */
    @Input
    public Property<DetailsSectionDisplay> getDetailsSectionDisplay()
    {
        return detailsSectionDisplay;
    }

    /**
     * Returns whether tracing defects fail the build.
     * 
     * @return the fail-build setting
     */
    @Input
    public Property<Boolean> getFailBuild()
    {
        return failBuild;
    }

    /**
     * Returns the status filter.
     * 
     * @return the status filter
     */
    @Input
    @Optional
    public SetProperty<ItemStatus> getFilterWantedStatuses()
    {
        return filterWantedStatuses;
    }

    private boolean shouldFailBuild()
    {
        return failBuild.getOrElse(true);
    }

    /** Traces the requirements and writes the report. */
    @TaskAction
    public void trace()
    {
        createReportOutputDir();
        final Oft oft = new OftRunner();
        final ImportSettings importSettings = getImportSettings();
        final List<SpecificationItem> importedItems = oft.importItems(importSettings);
        getLogger().info("Read {} spec items from {}", importedItems.size(),
                importSettings.getInputs());
        final List<LinkedSpecificationItem> linkedItems = oft.link(importedItems);
        final Trace trace = oft.trace(linkedItems);
        final Path reportPath = getOutputFileInternal().toPath();
        getLogger().info("Tracing result: {} total items, {} defects. Writing report to {}",
                trace.count(), trace.countDefects(), reportPath);
        oft.reportToPath(trace, reportPath, getReportSettings());
        if (trace.countDefects() > 0)
        {
            final String message = "Requirement tracing found " + trace.countDefects()
                    + " defects. See report at " + reportPath + " for details.";
            if (shouldFailBuild())
            {
                throw new IllegalStateException(message);
            }
            getLogger().warn(message);
        }
        else
        {
            getLogger().info("Requirement tracing completed successfully.");
        }
    }

    private ReportSettings getReportSettings()
    {
        getLogger().info("Report settings: verbosity={}, format={}, detailsSectionDisplay={}, colorScheme={}",
                reportVerbosity.get(), reportFormat.get(), detailsSectionDisplay.get(), reportColorScheme.getOrNull());
        final Builder builder = ReportSettings.builder()
                .verbosity(reportVerbosity.get())
                .outputFormat(reportFormat.get())
                .showOrigin(true)
                .newline(Newline.UNIX)
                .detailsSectionDisplay(detailsSectionDisplay.get())
                .colorScheme(reportColorScheme.getOrElse(DEFAULT_COLOR_SCHEME));
        return builder.build();
    }

    private ImportSettings getImportSettings()
    {
        return ImportSettings.builder()
                .addInputs(getAllImportFiles())
                .filter(getFilterSettings())
                .build();
    }

    private FilterSettings getFilterSettings()
    {
        final FilterSettings settings = FilterSettings.builder()
                .artifactTypes(filteredArtifactTypes.getOrElse(emptySet()))
                .tags(filteredTags.get())
                .withoutTags(filterAcceptsItemsWithoutTag.get())
                .wantedStatuses(filterWantedStatuses.get())
                .build();
        getLogger().info("Filter settings: artifactTypes={}, tags={}, acceptItemsWithoutTag={}",
                settings.getArtifactTypes(), settings.getTags(),
                settings.isArtifactTypeCriteriaSet());
        return settings;
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> importedRequirementPaths = importedRequirements.getFiles().stream()
                .map(File::toPath);
        final Stream<Path> inputDirPaths = Stream.of(requirementsFile.getAsFile().get().toPath());
        return Stream.concat(importedRequirementPaths, inputDirPaths).toList();
    }

    private void createReportOutputDir()
    {
        final File outputDir = getOutputFileInternal().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IllegalStateException("Error creating directory " + outputDir);
        }
    }

    private File getOutputFileInternal()
    {
        return outputFile.getAsFile().get();
    }
}
