package org.itsallcode.openfasttrace.gradle.task;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.itsallcode.openfasttrace.api.FilterSettings;
import org.itsallcode.openfasttrace.api.ReportSettings;
import org.itsallcode.openfasttrace.api.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.api.core.Newline;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.Trace;
import org.itsallcode.openfasttrace.api.importer.ImportSettings;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;
import org.itsallcode.openfasttrace.core.Oft;
import org.itsallcode.openfasttrace.core.OftRunner;

public class TraceTask extends DefaultTask
{
    private final RegularFileProperty requirementsFile = getProject().getObjects().fileProperty();
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);
    private final Property<String> reportFormat = getProject().getObjects().property(String.class);
    private final SetProperty<File> importedRequirements = getProject().getObjects()
            .setProperty(File.class);
    private final SetProperty<String> filteredArtifactTypes = getProject().getObjects()
            .setProperty(String.class);
    private final SetProperty<String> filteredTags = getProject().getObjects()
            .setProperty(String.class);
    private final Property<Boolean> filterAcceptsItemsWithoutTag = getProject().getObjects()
            .property(Boolean.class);

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

    @TaskAction
    public void trace() throws IOException
    {
        createReportOutputDir();
        final Oft oft = new OftRunner();
        final ImportSettings importSettings = getImportSettings();
        final List<SpecificationItem> importedItems = oft.importItems(importSettings);
        getLogger().info("Read {} spec items from {}", importedItems.size(),
                importSettings.getInputs());
        final List<LinkedSpecificationItem> linkedItems = oft.link(importedItems);
        final Trace trace = oft.trace(linkedItems);
        final Path report = getOutputFileInternal().toPath();
        getLogger().info("Tracing result: {} total items, {} defects. Writing report to {}",
                trace.count(), trace.countDefects(), report);
        oft.reportToPath(trace, report, getReportSettings());
    }

    private ReportSettings getReportSettings()
    {
        return ReportSettings.builder() //
                .verbosity(reportVerbosity.get()) //
                .outputFormat(reportFormat.get()) //
                .showOrigin(true) //
                .newline(Newline.UNIX) //
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
        getLogger().info("Filter: artifactTypes={}, tags={}, acceptItemsWithoutTag={}",
                filteredArtifactTypes.get(), filteredTags.get(),
                filterAcceptsItemsWithoutTag.get());
        return FilterSettings.builder() //
                .artifactTypes(filteredArtifactTypes.get()) //
                .tags(filteredTags.get()) //
                .withoutTags(filterAcceptsItemsWithoutTag.get()).build();
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> importedRequirementPaths = importedRequirements.get().stream()
                .map(File::toPath);
        final Stream<Path> inputDirPaths = Stream.of(requirementsFile.getAsFile().get().toPath());
        return Stream.concat(importedRequirementPaths, inputDirPaths).collect(toList());
    }

    private void createReportOutputDir() throws IOException
    {
        final File outputDir = getOutputFileInternal().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IOException("Error creating directory " + outputDir);
        }
    }

    private File getOutputFileInternal()
    {
        return outputFile.getAsFile().get();
    }
}
