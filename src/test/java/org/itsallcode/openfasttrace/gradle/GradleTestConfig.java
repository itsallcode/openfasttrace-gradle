package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null),
    /**
     * Version 8.5 is the first one that supports running on Java 21
     */
    EIGHT_FIVE("8.5");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }

    public boolean supportedWithJvm()
    {
        return true;
    }
}
