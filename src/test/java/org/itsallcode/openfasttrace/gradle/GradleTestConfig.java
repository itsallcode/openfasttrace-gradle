package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig {
    THIS_VERSION(null),
    /**
     * We support the latest Gradle version and the previous two. Older Gradle
     * versions do not work with Java 21 and above.
     */
    PREVIOUS_VERSION_MINUS_ONE("9.1.0"), PREVIOUS_VERSION("9.2.0"), CURRENT_VERSION("9.3.1");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }
}
