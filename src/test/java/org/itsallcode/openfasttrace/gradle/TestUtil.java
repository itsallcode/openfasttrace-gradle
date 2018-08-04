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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
            throw new AssertionError("Error reading from resource " + resourceName);
        }
    }

    public static void writeFile(Path file, String content)
    {
        try
        {
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        }
        catch (final IOException e)
        {
            throw new AssertionError("Error writing to file " + file);
        }
    }
}
