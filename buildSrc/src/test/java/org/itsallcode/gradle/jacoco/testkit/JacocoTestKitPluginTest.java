package org.itsallcode.gradle.jacoco.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JacocoTestKitPluginTest
{
    private static final String SETTINGS_FILE = "rootProject.name = 'jacoco-testkit-plugin-test'\n";
    private static final String BUILD_FILE_CONTENT = """
            repositories {
                mavenCentral()
            }

            tasks.register('verifyJacocoTestKitSetup') {
                doLast {
                    def agentConfiguration = configurations.getByName('jacocoTestKitRuntime')
                    assert agentConfiguration.canBeResolved
                    assert !agentConfiguration.canBeConsumed
                    assert agentConfiguration.dependencies.any {
                        it.group == 'org.jacoco' &&
                                it.name == 'org.jacoco.agent' &&
                                it.version == jacoco.toolVersion
                    }
                    def testTask = tasks.named('test').get()
                    assert testTask.taskDependencies.getDependencies(testTask).any {
                        it.name == 'generateJacocoTestKitProperties'
                    }
                }
            }
            """;
    private static final String BUILD_FILE_JAVA_FIRST = """
            plugins {
                id 'java'
                id 'org.itsallcode.jacoco-testkit'
            }

            """ + BUILD_FILE_CONTENT;
    private static final String BUILD_FILE_JACOCO_FIRST = """
            plugins {
                id 'org.itsallcode.jacoco-testkit'
            }

            apply plugin: 'java'

            """ + BUILD_FILE_CONTENT;

    @TempDir
    Path projectDir;

    @Test
    void generatesTestKitPropertiesWhenJavaPluginWasAppliedFirst() throws IOException
    {
        assertPluginSetup(BUILD_FILE_JAVA_FIRST);
    }

    @Test
    void generatesTestKitPropertiesWhenJavaPluginIsAppliedAfter() throws IOException
    {
        assertPluginSetup(BUILD_FILE_JACOCO_FIRST);
    }

    private void assertPluginSetup(final String buildFile) throws IOException
    {
        writeProjectFile("settings.gradle", SETTINGS_FILE);
        writeProjectFile("build.gradle", buildFile);

        final BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("verifyJacocoTestKitSetup", "generateJacocoTestKitProperties",
                        "--warning-mode", "fail")
                .build();

        assertNotNull(result.task(":generateJacocoTestKitProperties"));
        assertFalse(result.getOutput().contains("Configuration.setVisible(boolean)"));
        assertGeneratedTestKitProperties();
    }

    private void writeProjectFile(final String fileName, final String content) throws IOException
    {
        Files.writeString(projectDir.resolve(fileName), content);
    }

    private void assertGeneratedTestKitProperties() throws IOException
    {
        final Path propertiesPath = projectDir.resolve(
                "build/testkit/test/testkit-gradle.properties");
        assertTrue(Files.isRegularFile(propertiesPath),
                "The TestKit Gradle properties file should be generated");

        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(propertiesPath))
        {
            properties.load(reader);
        }

        final String jvmArgs = properties.getProperty("org.gradle.jvmargs");
        assertNotNull(jvmArgs);
        assertTrue(jvmArgs.startsWith("\"-javaagent:"),
                "The generated Gradle JVM arguments should include the JaCoCo agent");
        assertTrue(jvmArgs.contains("=destfile="),
                "The generated JaCoCo agent arguments should configure an execution data file");
        assertTrue(jvmArgs.endsWith(",append=true\""),
                "The generated JaCoCo agent should append to the test execution data");
        assertEquals(1, properties.size(),
                "The generated TestKit properties should only configure the JaCoCo agent");
    }
}
