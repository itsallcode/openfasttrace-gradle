package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null),
    /**
     * We support the latest Gradle version and the previous two.
     */
    EIGHT_EIGHT("8.8"), EIGHT_NINE("8.9"), EIGHT_TEN("8.10");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }
}
