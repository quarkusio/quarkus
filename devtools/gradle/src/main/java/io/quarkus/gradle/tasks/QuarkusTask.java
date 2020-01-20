package io.quarkus.gradle.tasks;

import org.gradle.api.DefaultTask;

import io.quarkus.gradle.QuarkusPluginExtension;

public abstract class QuarkusTask extends DefaultTask {

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        GradleLogger.logSupplier = this::getLogger;

        setDescription(description);
        setGroup("quarkus");
    }

    QuarkusPluginExtension extension() {
        if (extension == null) {
            extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        }
        return extension;
    }
}
