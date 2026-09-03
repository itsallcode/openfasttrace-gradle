package org.itsallcode.openfasttrace.gradle.task.classloader;

import java.util.Arrays;
import java.util.Objects;

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
}