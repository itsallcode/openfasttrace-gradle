package org.itsallcode.openfasttrace.gradle;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;

public class TestUtil
{
    public static Optional<String> readResource(final Class<?> clazz, final String resourceName)
    {
        final URL resource = clazz.getResource(resourceName);
        if (resource == null)
        {
            return Optional.empty();
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8.name())))
        {
            final StringBuilder b = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                b.append(line).append("\n");
            }
            return Optional.of(b.toString());
        }
        catch (final IOException e)
        {
            throw new AssertionError("Error reading from resource " + resourceName, e);
        }
    }

    public static void writeFile(final Path file, final String content)
    {
        try
        {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        }
        catch (final IOException e)
        {
            throw new AssertionError("Error writing to file " + file, e);
        }
    }

    public static String fileContent(final Path file)
    {
        try
        {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static void createDirs(final Path dir)
    {
        try
        {
            Files.createDirectories(dir);
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException("Failed to create directory " + dir, e);
        }
    }

    public static void assertFileContent(final Path file, final String... lines)
    {
        final String fileContent = TestUtil.fileContent(file);
        for (final String line : lines)
        {
            assertThat("Content of file " + file, fileContent, containsString(line));
        }
    }
}
