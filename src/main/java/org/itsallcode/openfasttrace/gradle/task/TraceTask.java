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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
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
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.itsallcode.openfasttrace.FilterSettings;
import org.itsallcode.openfasttrace.core.Trace;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;
import org.itsallcode.openfasttrace.importer.legacytag.config.LegacyTagImporterConfig;
import org.itsallcode.openfasttrace.importer.legacytag.config.PathConfig;
import org.itsallcode.openfasttrace.mode.ReportMode;
import org.itsallcode.openfasttrace.report.ReportVerbosity;

public class TraceTask extends DefaultTask
{
    @InputDirectory
    public Supplier<Set<File>> inputDirectories = () -> emptySet();
    @OutputFile
    public final RegularFileProperty outputFile = getProject().getLayout().fileProperty();
    @Input
    public final Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);
    @Input
    public Supplier<String> reportFormat;
    @Input
    public Supplier<List<TagPathConfiguration>> pathConfig = () -> emptyList();
    @Input
    public Supplier<Set<File>> importedRequirements;
    @Input
    public Supplier<Set<String>> filteredArtifactTypes;
    @Input
    public Supplier<Set<String>> filteredTags;

    @TaskAction
    public void trace() throws IOException
    {
        createReportOutputDir();
        final ReportMode reporter = new ReportMode();
        final Trace trace = reporter //
                .addInputs(getAllImportFiles()) //
                .setReportVerbosity(reportVerbosity.get()) //
                .setLegacyTagImporterPathConfig(getPathConfig()) //
                .setFilters(getFilterSettings()) //
                .trace();
        reporter.reportToFileInFormat(trace, getOuputFile().toPath(), reportFormat.get());
    }

    private FilterSettings getFilterSettings()
    {
        return new FilterSettings.Builder() //
                .artifactTypes(filteredArtifactTypes.get()) //
                .tags(filteredTags.get()) //
                .build();
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> importedRequirementPaths = importedRequirements.get().stream()
                .map(File::toPath);
        final Stream<Path> inputDirPaths = inputDirectories.get().stream() //
                .map(File::toPath);
        final Stream<Path> inputTagPaths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPaths);
        final List<Path> files = Stream
                .concat(importedRequirementPaths, Stream.concat(inputDirPaths, inputTagPaths))
                .peek(p -> System.out.println("- " + p)).collect(toList());
        return files;
    }

    @Internal
    private LegacyTagImporterConfig getPathConfig()
    {
        final List<PathConfig> paths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPathConfig).collect(toList());
        getLogger().info("Got {} path configurations:\n{}", paths.size(),
                paths.stream().map(this::formatPathConfig).collect(joining("\n")));
        return new LegacyTagImporterConfig(paths);
    }

    private String formatPathConfig(PathConfig config)
    {
        return " - " + config.getDescription() + " (type " + config.getTagArtifactType()
                + "): covers '" + config.getCoveredItemArtifactType() + "', prefix: '"
                + config.getCoveredItemNamePrefix() + "'";
    }

    private void createReportOutputDir() throws IOException
    {
        final File outputDir = getOuputFile().getParentFile();
        if (outputDir.exists())
        {
            return;
        }
        if (!outputDir.mkdirs())
        {
            throw new IOException("Error creating directory " + outputDir);
        }
    }

    @Internal
    private File getOuputFile()
    {
        return outputFile.getAsFile().get();
    }
}
