package org.itsallcode.openfasttrace.api.importer;

import java.util.Collection;

/**
 * Compatibility shim for RegexMatchingImporterFactory which was renamed to
 * AbstractRegexMatchingImporterFactory in OpenFastTrace 4.5.0.
 * <p>
 * Shim can be removed when the following issue is fixed:
 * <a href="https://github.com/itsallcode/openfasttrace-asciidoc-plugin/issues/27">
 * itsallcode/openffasttrace-asciidoc-plugin # 27
 * </a>
 * </p>
 * Copied from
 * <a href=
 * "https://github.com/itsallcode/openfasttrace-maven-plugin/blob/main/src/main/java/org/itsallcode/openfasttrace/api/importer/RegexMatchingImporterFactory.java">OpenFastTrace
 * Maven Plugin</a>
 * 
 * @deprecated use {@link AbstractRegexMatchingImporterFactory} instead.
 */
@Deprecated(since = "3.2.0", forRemoval = true)
@SuppressWarnings("java:S118") // Shim class. Ignore name convention.
public abstract class RegexMatchingImporterFactory extends AbstractRegexMatchingImporterFactory
{
    /**
     * Constructs a new RegexMatchingImporterFactory with the specified file extensions.
     * 
     * @param extensions
     *            the file extensions to be associated with this importer factory
     * @deprecated use {@link AbstractRegexMatchingImporterFactory} instead.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    protected RegexMatchingImporterFactory(final String... extensions)
    {
        super(extensions);
    }

    /**
     * Constructs a new RegexMatchingImporterFactory with the specified file extensions.
     * 
     * @param extensions
     *            the file extensions to be associated with this importer factory
     * @deprecated use {@link AbstractRegexMatchingImporterFactory} instead.
     */
    @Deprecated(since = "3.2.0", forRemoval = true)
    protected RegexMatchingImporterFactory(final Collection<String> extensions)
    {
        super(extensions);
    }
}
