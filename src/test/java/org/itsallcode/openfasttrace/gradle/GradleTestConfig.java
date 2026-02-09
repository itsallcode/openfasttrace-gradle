package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig {
    THIS_VERSION(null),
    /**
     * We support the latest Gradle version and the previous two.
     */
    PREVIOUS_VERSION_MINUS_ONE("7.6.6"), PREVIOUS_VERSION("8.14.1"), CURRENT_VERSION("9.3.1");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }
}
