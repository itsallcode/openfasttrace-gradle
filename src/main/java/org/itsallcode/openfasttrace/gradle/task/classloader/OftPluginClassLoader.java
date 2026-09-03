package org.itsallcode.openfasttrace.gradle.task.classloader;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.itsallcode.openfasttrace.core.OftRunner;

/** Runs OpenFastTrace operations with additional plugin artifacts on the context classpath. */
public final class OftPluginClassLoader
{
    private static final Logger LOG = Logging.getLogger(OftPluginClassLoader.class);

    private OftPluginClassLoader()
    {
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
            catch (final IOException e)
            {
                LOG.warn("Could not close OpenFastTrace plugin classloader", e);
            }
        }
    }

    private static URLClassLoader createClassLoader(final FileCollection pluginFiles, final ClassLoader parent)
    {
        final URL[] pluginUrls = pluginFiles.getFiles().stream()
                .map(File::toURI)
                .map(OftPluginClassLoader::toUrl)
                .toArray(URL[]::new);
        final String pluginUrlsString = Arrays.toString(pluginUrls);
        return new ChildFirstClassLoader("ChildFirst ClassLoader for " + pluginUrlsString, pluginUrls, parent);
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
}
