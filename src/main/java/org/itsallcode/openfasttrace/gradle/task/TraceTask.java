package org.itsallcode.openfasttrace.gradle.task;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.itsallcode.openfasttrace.api.*;
import org.itsallcode.openfasttrace.api.core.*;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftRunner;

@SuppressWarnings("this-escape")
public class TraceTask extends DefaultTask
{
    private final RegularFileProperty requirementsFile = getProject().getObjects().fileProperty();
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);
    private final Property<String> reportFormat = getProject().getObjects().property(String.class);
    private final Property<DetailsSectionDisplay> detailsSectionDisplay = getProject().getObjects()
            .property(DetailsSectionDisplay.class);
    private final SetProperty<File> importedRequirements = getProject().getObjects()
            .setProperty(File.class);
    private final SetProperty<String> filteredArtifactTypes = getProject().getObjects()
            .setProperty(String.class);
    private final SetProperty<String> filteredTags = getProject().getObjects()
            .setProperty(String.class);
    private final Property<Boolean> filterAcceptsItemsWithoutTag = getProject().getObjects()
            .property(Boolean.class);
    private final Property<Boolean> failBuild = getProject().getObjects().property(Boolean.class);

    @InputFile
    public RegularFileProperty getRequirementsFile()
    {
        return requirementsFile;
    }

    @OutputFile
    public RegularFileProperty getOutputFile()
    {
        return outputFile;
    }

    @Input
    public Property<ReportVerbosity> getReportVerbosity()
    {
        return reportVerbosity;
    }

    @Input
    public Property<String> getReportFormat()
    {
        return reportFormat;
    }

    @Input
    public SetProperty<File> getImportedRequirements()
    {
        return importedRequirements;
    }

    @Input
    @Optional
    public SetProperty<String> getFilteredArtifactTypes()
    {
        return filteredArtifactTypes;
    }

    @Input
    public SetProperty<String> getFilteredTags()
    {
        return filteredTags;
    }

    @Input
    public Property<Boolean> getFilterAcceptsItemsWithoutTag()
    {
        return filterAcceptsItemsWithoutTag;
    }

    @Input
    public Property<DetailsSectionDisplay> getDetailsSectionDisplay()
    {
        return detailsSectionDisplay;
    }

    @Input
    public Property<Boolean> getFailBuild()
    {
        return failBuild;
    }

    private boolean shouldFailBuild()
    {
        return failBuild.getOrElse(true);
    }

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
        getLogger().info("Report settings: verbosity={}, format={}, detailsSectionDisplay={}",
                reportVerbosity.get(), reportFormat.get(), detailsSectionDisplay.get());
        return ReportSettings.builder() //
                .verbosity(reportVerbosity.get()) //
                .outputFormat(reportFormat.get()) //
                .showOrigin(true) //
                .newline(Newline.UNIX) //
                .detailsSectionDisplay(detailsSectionDisplay.get()) //
                .build();
    }

    private ImportSettings getImportSettings()
    {
        return ImportSettings.builder() //
                .addInputs(getAllImportFiles()) //
                .filter(getFilterSettings()) //
                .build();
    }

    private FilterSettings getFilterSettings()
    {
        final FilterSettings settings = FilterSettings.builder() //
                .artifactTypes(filteredArtifactTypes.getOrElse(emptySet())) //
                .tags(filteredTags.get()) //
                .withoutTags(filterAcceptsItemsWithoutTag.get()).build();
        getLogger().info("Filter settings: artifactTypes={}, tags={}, acceptItemsWithoutTag={}",
                settings.getArtifactTypes(), settings.getTags(),
                settings.isArtifactTypeCriteriaSet());
        return settings;
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> importedRequirementPaths = importedRequirements.get().stream()
                .map(File::toPath);
        final Stream<Path> inputDirPaths = Stream.of(requirementsFile.getAsFile().get().toPath());
        return Stream.concat(importedRequirementPaths, inputDirPaths).collect(toList());
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
