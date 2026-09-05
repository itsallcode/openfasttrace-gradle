package org.itsallcode.openfasttrace.gradle.task.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.*;

final class ParentClassLoader extends ClassLoader
{
    private final ClassLoader[] parents;

    ParentClassLoader(final ClassLoader... parents)
    {
        super(null);
        this.parents = Arrays.stream(parents)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(ClassLoader[]::new);
    }

    @Override
    @SuppressWarnings("java:S3032") // Explicit loading is required to delegate to multiple independent parents.
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
    {
        for (final ClassLoader parent : parents)
        {
            try
            {
                return Class.forName(name, resolve, parent);
            }
            catch (final ClassNotFoundException e)
            {
                // Ignore and try the next parent class loader‚
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(final String name)
    {
        for (final ClassLoader parent : parents)
        {
            final URL resource = parent.getResource(name);
            if (resource != null)
            {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException
    {
        final List<URL> resources = Arrays.stream(parents)
                .map(parent -> getResources(parent, name))
                .flatMap(List::stream)
                .toList();
        return Collections.enumeration(resources);
    }

    private static List<URL> getResources(final ClassLoader parent, final String name)
    {
        try
        {
            return Collections.list(parent.getResources(name));
        }
        catch (final IOException e)
        {
            throw new IllegalStateException("Could not get resources for " + name, e);
        }
    }
}
