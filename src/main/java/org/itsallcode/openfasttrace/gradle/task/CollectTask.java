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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.itsallcode.openfasttrace.exporter.specobject.SpecobjectExporterFactory;
import org.itsallcode.openfasttrace.gradle.config.TagPathConfiguration;
import org.itsallcode.openfasttrace.importer.tag.config.PathConfig;
import org.itsallcode.openfasttrace.importer.tag.config.TagImporterConfig;
import org.itsallcode.openfasttrace.mode.ConvertMode;

public class CollectTask extends DefaultTask
{
    @InputDirectory
    public Supplier<Set<File>> inputDirectories = Collections::emptySet;
    @OutputFile
    public final RegularFileProperty outputFile = getProject().getLayout().fileProperty();

    @Input
    public Supplier<List<TagPathConfiguration>> pathConfig = Collections::emptyList;

    @TaskAction
    public void collectRequirements() throws IOException
    {
        createReportOutputDir();
        final ConvertMode converter = new ConvertMode();
        converter //
                .addInputs(getAllImportFiles()) //
                .setLegacyTagImporterPathConfig(getPathConfigInternal()) //
                .convertToFileInFormat(getOuputFileInternal().toPath(),
                        SpecobjectExporterFactory.SUPPORTED_FORMAT);
    }

    private List<Path> getAllImportFiles()
    {
        final Stream<Path> inputDirPaths = inputDirectories.get().stream() //
                .map(File::toPath);
        final Stream<Path> inputTagPaths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPaths);
        return Stream.concat(inputDirPaths, inputTagPaths).collect(toList());
    }

    private TagImporterConfig getPathConfigInternal()
    {
        final List<PathConfig> paths = pathConfig.get().stream()
                .flatMap(TagPathConfiguration::getPathConfig).collect(toList());
        if (getLogger().isInfoEnabled())
        {
            getLogger().info("Got {} path configurations:\n{}", paths.size(),
                    paths.stream().map(this::formatPathConfig).collect(joining("\n")));
        }
        return new TagImporterConfig(paths);
    }

    private String formatPathConfig(PathConfig config)
    {
        return " - " + config.getDescription() + " (type " + config.getTagArtifactType()
                + "): covers '" + config.getCoveredItemArtifactType() + "', prefix: '"
                + config.getCoveredItemNamePrefix() + "'";
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
        return outputFile.getAsFile().get();
    }
}
