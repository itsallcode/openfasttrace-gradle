package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null),
    /**
     * We support the latest Gradle version and the previous two.
     */
    EIGHT_SIX("8.6"), EIGHT_SEVEN("8.7"), EIGHT_EIGHT("8.8");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }
}
