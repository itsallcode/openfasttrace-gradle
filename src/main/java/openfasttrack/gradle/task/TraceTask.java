/**
 * openfasttrack-gradle - Gradle plugin for tracing requirements using OpenFastTrack
 * Copyright (C) 2017 Hamster community <christoph at users.sourceforge.net>
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
package openfasttrack.gradle.task;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import openfasttrack.cli.CliArguments;
import openfasttrack.cli.commands.TraceCommand;
import openfasttrack.report.ReportVerbosity;

public class TraceTask extends DefaultTask
{
    @InputDirectory
    public final ConfigurableFileCollection inputDirectories = getProject().files();
    @OutputFile
    public final RegularFileProperty outputFile = getProject().getLayout().fileProperty();
    @Input
    public Property<ReportVerbosity> reportVerbosity = getProject().getObjects()
            .property(ReportVerbosity.class);

    @TaskAction
    public void trace() throws IOException
    {
        createReportOutputDir();
        final CliArguments args = buildTraceCommandArgs();
        new TraceCommand(args).start();
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

    private CliArguments buildTraceCommandArgs()
    {
        final CliArguments args = new CliArguments();
        args.setOutputFile(getOuputFile().getAbsolutePath());
        args.setReportVerbosity(reportVerbosity.get());
        args.setUnnamedValues(getUnnamedArgs());
        getLogger().info("Output file: {}", args.getOutputFile());
        return args;
    }

    @Internal
    private File getOuputFile()
    {
        return outputFile.getAsFile().get();
    }

    @Internal
    private List<String> getUnnamedArgs()
    {
        final List<String> unnamedValueArgs = new ArrayList<>();
        unnamedValueArgs.add(TraceCommand.COMMAND_NAME);
        unnamedValueArgs.addAll(getInputDirPaths());
        return unnamedValueArgs;
    }

    @Internal
    private List<String> getInputDirPaths()
    {
        return inputDirectories.getFiles().stream() //
                .map(File::getAbsolutePath) //
                .collect(toList());
    }
}
