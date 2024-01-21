package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null), EIGHT("8.0.2");

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
