package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null), SIX_ZERO("6.0");

    public final String gradleVersion;

    private GradleTestConfig(String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }
}
