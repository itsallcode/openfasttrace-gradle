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
package org.itsallcode.openfasttrace.gradle.task;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.itsallcode.openfasttrace.FilterSettings;
import org.itsallcode.openfasttrace.ImportSettings;
import org.itsallcode.openfasttrace.Oft;
import org.itsallcode.openfasttrace.ReportSettings;
import org.itsallcode.openfasttrace.core.LinkedSpecificationItem;
import org.itsallcode.openfasttrace.core.Newline;
import org.itsallcode.openfasttrace.core.OftRunner;
import org.itsallcode.openfasttrace.core.SpecificationItem;
import org.itsallcode.openfasttrace.core.Trace;
import org.itsallcode.openfasttrace.gradle.util.DeprecationUtil;
import org.itsallcode.openfasttrace.report.ReportVerbosity;

public class TraceTask extends DefaultTask
{
    @InputFile
    public final RegularFileProperty requirementsFile = DeprecationUtil
            .createFileProperty(getProject());
    @OutputFile
    public final RegularFileProperty outputFile = DeprecationUtil.createFileProperty(getProject());
    @Input
    public final Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);
    @Input
    public Supplier<String> reportFormat;
    @Input
    public Supplier<Set<File>> importedRequirements;
    @Input
    public Supplier<Set<String>> filteredArtifactTypes;
    @Input
    public Supplier<Set<String>> filteredTags;
    @Input
    public Supplier<Boolean> filterAcceptsItemsWithoutTag;

    @TaskAction
    public void trace() throws IOException
    {
        createReportOutputDir();
        final Oft oft = new OftRunner();
        final List<SpecificationItem> importedItems = oft.importItems(getImportSettings());
        final List<LinkedSpecificationItem> linkedItems = oft.link(importedItems);
        final Trace trace = oft.trace(linkedItems);
        oft.reportToPath(trace, getOuputFileInternal().toPath(), getReportSettings());
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
        return new FilterSettings.Builder() //
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
        final File outputDir = getOuputFileInternal().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IOException("Error creating directory " + outputDir);
        }
    }

    private File getOuputFileInternal()
    {
        if (outputFile.isPresent())
        {
            return outputFile.getAsFile().get();
        }
        return getDefaultOutputFile();
    }

    private File getDefaultOutputFile()
    {
        final String extension = reportFormat.get().equals("html") ? "html" : "txt";
        return new File(getProject().getBuildDir(), "reports/tracing." + extension);
    }
}
