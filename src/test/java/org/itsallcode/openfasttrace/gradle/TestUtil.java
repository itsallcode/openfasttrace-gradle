package org.itsallcode.openfasttrace.gradle;

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
}
