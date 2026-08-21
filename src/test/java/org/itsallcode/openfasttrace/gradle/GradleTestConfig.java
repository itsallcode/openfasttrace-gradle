package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    THIS_VERSION(null),

    /**
     * We support the latest Gradle version and the previous two. Older Gradle
     * versions do not work with Java 21 and above.
     * <p>
     * Version 8 does not support testkit with configuration cache, see
     * https://github.com/gradle/gradle/issues/25979
     */
    // PREVIOUS_VERSION("8.14.5"),
    CURRENT_VERSION("9.7.1");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }
}
