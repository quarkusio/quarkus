package io.quarkus.gradle.tasks;

import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.gradle.QuarkusPlugin;

public class QuarkusTestNative extends Test {

    public QuarkusTestNative() {
        setDescription("Runs native image tests");
        setGroup("verification");

        JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaPlugin.getSourceSets();
        SourceSet sourceSet = sourceSets.findByName(QuarkusPlugin.NATIVE_TEST_SOURCE_SET_NAME);

        setTestClassesDirs(sourceSet.getOutput().getClassesDirs());
        setClasspath(sourceSet.getRuntimeClasspath());
    }
}
