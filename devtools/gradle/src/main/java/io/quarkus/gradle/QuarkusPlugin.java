package io.quarkus.gradle;

import java.util.Collections;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateConfig;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusNative;
import io.quarkus.gradle.tasks.QuarkusTestConfig;
import io.quarkus.gradle.tasks.QuarkusTestNative;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        verifyGradleVersion();
        // register extension
        project.getExtensions().create("quarkus", QuarkusPluginExtension.class, project);

        registerTasks(project);
    }

    private void registerTasks(Project project) {
        TaskContainer tasks = project.getTasks();
        tasks.create("listExtensions", QuarkusListExtensions.class);
        tasks.create("addExtension", QuarkusAddExtension.class);

        Task quarkusBuild = tasks.create("quarkusBuild", QuarkusBuild.class);
        Task quarkusGenerateConfig = tasks.create("generateConfig", QuarkusGenerateConfig.class);
        Task quarkusDev = tasks.create("quarkusDev", QuarkusDev.class);

        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(quarkusBuild));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    Task classesTask = tasks.getByName(JavaPlugin.CLASSES_TASK_NAME);
                    quarkusDev.dependsOn(classesTask);
                    quarkusBuild.dependsOn(classesTask);
                });

        Task buildNative = tasks.create("buildNative", QuarkusNative.class).dependsOn(quarkusBuild);

        // set up the source set for the testNative
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaPlugin != null) {
            SourceSetContainer sourceSets = javaPlugin.getSourceSets();
            SourceSet nativeTestSourceSet = sourceSets.create("native-test"); // this name has to be the same as the directory in which the tests reside
            SourceSetOutput mainSourceSetOutput = sourceSets.getByName("main").getOutput();
            SourceSetOutput testSourceSetOutput = sourceSets.getByName("test").getOutput();
            nativeTestSourceSet.setCompileClasspath(
                    nativeTestSourceSet.getCompileClasspath().plus(mainSourceSetOutput).plus(testSourceSetOutput));
            nativeTestSourceSet.setRuntimeClasspath(
                    nativeTestSourceSet.getRuntimeClasspath().plus(mainSourceSetOutput).plus(testSourceSetOutput));

            // create a custom configuration to be used for the dependencies of the testNative task
            ConfigurationContainer configurations = project.getConfigurations();
            configurations.maybeCreate("nativeTestImplementation").extendsFrom(configurations.findByName("implementation"));
            configurations.maybeCreate("nativeTestRuntimeOnly").extendsFrom(configurations.findByName("runtimeOnly"));

            Task testNative = tasks.create("testNative", QuarkusTestNative.class).dependsOn(buildNative);
            testNative.setShouldRunAfter(Collections.singletonList(tasks.findByName("test")));

            tasks.getByName("check").dependsOn(testNative);
        }

        final QuarkusTestConfig quarkusTestConfig = tasks.create("quarkusTestConfig", QuarkusTestConfig.class);
        tasks.withType(Test.class).forEach(t -> {
            // Quarkus test configuration task which should be executed before any Quarkus test
            t.dependsOn(quarkusTestConfig);
            // also make each task use the JUnit platform since it's the only supported test environment
            t.useJUnitPlatform();
        });
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 5.0 or later. Current version is: " +
                    GradleVersion.current());
        }
    }
}
