package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null), SEVEN_ZERO("7.0"), SEVEN_FIVE("7.6");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }

    public boolean supportedWithJvm()
    {
        final boolean isJava11 = System.getProperty("java.version").startsWith("11");
        if (this == SEVEN_ZERO)
        {
            return isJava11;
        }
        return true;
    }
}
