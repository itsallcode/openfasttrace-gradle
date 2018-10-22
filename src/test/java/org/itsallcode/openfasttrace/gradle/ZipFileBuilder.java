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
package org.itsallcode.openfasttrace.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFileBuilder implements AutoCloseable
{
    private final ZipOutputStream zipOutputStream;

    private ZipFileBuilder(ZipOutputStream zipOutputStream)
    {
        this.zipOutputStream = zipOutputStream;
    }

    public static ZipFileBuilder create(Path target) throws IOException
    {
        return create(target.toFile());
    }

    public static ZipFileBuilder create(File target) throws IOException
    {
        final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(target),
                StandardCharsets.UTF_8);
        return new ZipFileBuilder(zipOutputStream);
    }

    public ZipFileBuilder addEntry(final String entryName, Path file) throws IOException
    {
        addEntry(entryName, Files.readAllBytes(file));
        return this;
    }

    public ZipFileBuilder addEntry(final String entryName, final byte[] data) throws IOException
    {
        this.zipOutputStream.putNextEntry(new ZipEntry(entryName));
        if (data != null)
        {
            this.zipOutputStream.write(data);
        }
        this.zipOutputStream.closeEntry();
        return this;
    }

    public void build() throws IOException
    {
        close();
    }

    @Override
    public void close() throws IOException
    {
        zipOutputStream.close();
    }
}
