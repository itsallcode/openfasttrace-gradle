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
