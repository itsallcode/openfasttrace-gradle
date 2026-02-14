package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    THIS_VERSION(null),
    /**
     * We support the latest Gradle version and the previous two. Older Gradle
     * versions do not work with Java 21 and above.
     */
    PREVIOUS_VERSION("8.14.4"), CURRENT_VERSION("9.3.1");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }
}
