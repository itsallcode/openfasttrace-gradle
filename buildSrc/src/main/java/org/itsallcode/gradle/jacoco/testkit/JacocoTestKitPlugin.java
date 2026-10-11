package org.itsallcode.gradle.jacoco.testkit;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.WriteProperties;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;

public final class JacocoTestKitPlugin implements Plugin<Project>
{
    private static final String JACOCO_RUNTIME_CONFIGURATION = "jacocoTestKitRuntime";
    private static final String TEST_TASK_NAME = "test";
    private static final String TEST_KIT_DIRECTORY = "testkit/test";
    private static final String TEST_KIT_PROPERTIES = "testkit-gradle.properties";

    @Override
    public void apply(final Project project)
    {
        project.getPluginManager().apply("jacoco");

        final Configuration jacocoRuntime = project.getConfigurations().maybeCreate(JACOCO_RUNTIME_CONFIGURATION);
        jacocoRuntime.setCanBeConsumed(false);
        jacocoRuntime.setCanBeResolved(true);
        jacocoRuntime.setDescription("JaCoCo agent for Gradle TestKit builds");

        project.afterEvaluate(ignored -> addJacocoAgentDependency(project));
        project.getPluginManager().withPlugin("java", ignored -> configureTestTask(project, jacocoRuntime));
    }

    private static void addJacocoAgentDependency(final Project project)
    {
        final JacocoPluginExtension jacoco = project.getExtensions().getByType(JacocoPluginExtension.class);
        project.getDependencies().add(JACOCO_RUNTIME_CONFIGURATION,
                "org.jacoco:org.jacoco.agent:" + jacoco.getToolVersion() + ":runtime");
    }

    private static void configureTestTask(final Project project, final Configuration jacocoRuntime)
    {
        final TaskProvider<Test> testTask = project.getTasks().named(TEST_TASK_NAME, Test.class);
        final File testKitDirectory = project.getLayout().getBuildDirectory().dir(TEST_KIT_DIRECTORY).get().getAsFile();
        final File propertiesFile = new File(testKitDirectory, TEST_KIT_PROPERTIES);
        final TaskProvider<WriteProperties> generateProperties = project.getTasks()
                .register("generateJacocoTestKitProperties", WriteProperties.class, task -> {
                    task.setGroup("verification");
                    task.setDescription(
                            "Generates Gradle properties to instrument TestKit builds with JaCoCo");
                    task.getDestinationFile().set(propertiesFile);
                    task.property("org.gradle.jvmargs", testKitJvmArgs(testTask, jacocoRuntime));
                });

        project.getDependencies().add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, project.files(testKitDirectory));
        testTask.configure(task -> task.dependsOn(generateProperties));
    }

    private static String testKitJvmArgs(final TaskProvider<Test> testTask, final Configuration jacocoRuntime)
    {
        final File agentJar = jacocoRuntime.getSingleFile();
        final JacocoTaskExtension jacoco = testTask.get().getExtensions().getByType(JacocoTaskExtension.class);
        final File executionData = jacoco.getDestinationFile();
        if (executionData == null)
        {
            throw new IllegalStateException("JaCoCo execution data file is not configured for the test task");
        }
        return "\"-javaagent:" + agentJar.getAbsolutePath() + "=destfile=" + executionData.getAbsolutePath()
                + ",append=true\"";
    }
}
