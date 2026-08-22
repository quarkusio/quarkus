package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.gradle.application.dsl.QuarkusApplicationConfigInputs;
import io.quarkus.gradle.application.dsl.QuarkusApplicationForkOptions;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBaseTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationEffectiveConfigTask;

final class TaskRegistrationSupport {

    static final String QUARKUS_APPLICATION_GROUP = "quarkus application";

    private TaskRegistrationSupport() {
    }

    static void configureForkOptions(QuarkusApplicationForkOptions target, QuarkusApplicationForkOptions source) {
        target.getJvmArgs().set(source.getJvmArgs());
        target.getSystemProperties().set(source.getSystemProperties());
        target.getEnvironment().set(source.getEnvironment());
        target.getMinHeapSize().set(source.getMinHeapSize());
        target.getMaxHeapSize().set(source.getMaxHeapSize());
        target.getEnableAssertions().set(source.getEnableAssertions());
        target.getDebug().set(source.getDebug());
        target.getDefaultCharacterEncoding().set(source.getDefaultCharacterEncoding());
    }

    static void configureJavaClasspath(Project project, QuarkusApplicationBuildTask task) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java == null) {
            return;
        }
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        task.getRuntimeClasspath().from(mainSourceSet.getCompileClasspath(), mainSourceSet.getRuntimeClasspath(),
                mainSourceSet.getAnnotationProcessorPath(), mainSourceSet.getResources());
    }

    static void configureJavaSourceDirectories(Project project, QuarkusApplicationEffectiveConfigTask task) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java == null) {
            return;
        }
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        task.getSourceDirectories().from(mainSourceSet.getResources().getSourceDirectories());
    }

    static void configureConfigInputs(QuarkusApplicationBaseTask task,
            QuarkusApplicationConfigInputs configInputs) {
        task.getGradlePropertyPrefixes().set(configInputs.getProjectProperties().getPrefixes());
        task.getGradlePropertyNames().set(configInputs.getProjectProperties().getNames());
        task.getSystemPropertyPrefixes().set(configInputs.getSystemProperties().getPrefixes());
        task.getSystemPropertyNames().set(configInputs.getSystemProperties().getNames());
        task.getEnvironmentVariablePrefixes().set(configInputs.getEnvironmentVariables().getPrefixes());
        task.getEnvironmentVariableNames().set(configInputs.getEnvironmentVariables().getNames());
        task.getLegacyAmbientConfigCapture().set(configInputs.getLegacyAmbientConfigCapture());
        if (configInputs.getLegacyAmbientConfigCapture().getOrElse(false)) {
            task.notCompatibleWithConfigurationCache(
                    "Legacy ambient config capture reads all Gradle properties, JVM system properties, "
                            + "and environment variables.");
        }
    }
}
