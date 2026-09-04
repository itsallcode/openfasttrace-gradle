package org.itsallcode.openfasttrace.gradle.task.classloader;

import java.io.IOException;
import java.net.*;
import java.util.*;

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
        final ClassLoader parent = new ParentClassLoader(OftRunner.class.getClassLoader(), originalClassLoader);
        final URLClassLoader pluginClassLoader = createClassLoader(pluginFiles, parent);
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
        final Set<URL> pluginUrls = new HashSet<>(pluginFiles.getFiles().stream()
                .map(file -> file.toPath().toUri())
                .map(OftPluginClassLoader::toUrl)
                .toList());
        // OFT only accepts service providers loaded by the classloader that discovered them.
        // Add OFT's built-in provider JARs to this loader so they are not filtered out.
        addServiceProviderJars(parent, pluginUrls,
                "org.itsallcode.openfasttrace.api.exporter.ExporterFactory");
        addServiceProviderJars(parent, pluginUrls,
                "org.itsallcode.openfasttrace.api.importer.ImporterFactory");
        final URL[] urls = pluginUrls.toArray(URL[]::new);
        final String pluginUrlsString = Arrays.toString(urls);
        return new ChildFirstClassLoader("ChildFirst ClassLoader for " + pluginUrlsString, urls, parent);
    }

    private static void addServiceProviderJars(final ClassLoader parent, final Set<URL> urls,
            final String serviceName)
    {
        try
        {
            final String resourceName = "META-INF/services/" + serviceName;
            for (final URL resource : java.util.Collections.list(parent.getResources(resourceName)))
            {
                final URLConnection connection = resource.openConnection();
                if (connection instanceof final JarURLConnection jarConnection)
                {
                    urls.add(jarConnection.getJarFileURL());
                }
            }
        }
        catch (final IOException e)
        {
            throw new IllegalStateException("Could not locate service provider jars for " + serviceName, e);
        }
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
