package org.itsallcode.openfasttrace.gradle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestUtil
{
    public static String readResource(Class<?> clazz, String resourceName)
    {
        final URL resource = clazz.getResource(resourceName);
        if (resource == null)
        {
            throw new AssertionError("Resource '" + resourceName + "' not found");
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
            return b.toString();
        }
        catch (final IOException e)
        {
            throw new AssertionError("Error reading from resource " + resourceName, e);
        }
    }

    public static void writeFile(Path file, String content)
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
