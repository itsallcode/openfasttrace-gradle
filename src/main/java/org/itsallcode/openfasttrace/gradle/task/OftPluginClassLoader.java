package org.itsallcode.openfasttrace.gradle.task;

import java.io.File;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;

import org.gradle.api.file.FileCollection;
import org.itsallcode.openfasttrace.core.OftRunner;

/** Runs OpenFastTrace operations with additional plugin artifacts on the context classpath. */
public final class OftPluginClassLoader
{
    private OftPluginClassLoader()
    {
        super();
    }

    /**
     * Runs an operation with the given plugin files available to service loading.
     *
     * @param pluginFiles
     *            plugin artifacts to expose
     * @param action
     *            operation to run
     */
    public static void runWithPlugins(final FileCollection pluginFiles, final Runnable action)
    {
        if (pluginFiles.isEmpty())
        {
            action.run();
            return;
        }

        final Thread thread = Thread.currentThread();
        final ClassLoader originalClassLoader = thread.getContextClassLoader();
        final URLClassLoader pluginClassLoader = createClassLoader(pluginFiles,
                new ParentClassLoader(originalClassLoader, OftRunner.class.getClassLoader()));
        thread.setContextClassLoader(pluginClassLoader);
        try
        {
            action.run();
        }
        finally
        {
            thread.setContextClassLoader(originalClassLoader);
            try
            {
                pluginClassLoader.close();
            }
            catch (final java.io.IOException e)
            {
                throw new IllegalStateException("Could not close OpenFastTrace plugin classloader", e);
            }
        }
    }

    private static URLClassLoader createClassLoader(final FileCollection pluginFiles,
            final ClassLoader parent)
    {
        final URL[] pluginUrls = pluginFiles.getFiles().stream()
                .map(File::toURI)
                .map(OftPluginClassLoader::toUrl)
                .toArray(URL[]::new);

        return new ChildFirstClassLoader("ChildFirst ClassLoader for " + Arrays.toString(pluginUrls), pluginUrls,
                parent);
    }

    private static URL toUrl(final URI uri)
    {
        try
        {
            return uri.toURL();
        }
        catch (final MalformedURLException e)
        {
            throw new IllegalArgumentException("Invalid plugin file URL: " + uri, e);
        }
    }

    private static final class ParentClassLoader extends ClassLoader
    {
        private final ClassLoader[] parents;

        private ParentClassLoader(final ClassLoader... parents)
        {
            super(null);
            this.parents = Arrays.stream(parents)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toArray(ClassLoader[]::new);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException
        {
            for (final ClassLoader parent : parents)
            {
                try
                {
                    return Class.forName(name, resolve, parent);
                }
                catch (final ClassNotFoundException e)
                {}
            }
            throw new ClassNotFoundException(name);
        }
    }
}
