package io.quarkus.gradle.tasks;

import org.gradle.api.DefaultTask;

import io.quarkus.gradle.QuarkusPluginExtension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public abstract class QuarkusTask extends DefaultTask {

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        GradleLogger.logSupplier = this::getLogger;

        setDescription(description);
        setGroup("quarkus");
    }

    QuarkusPluginExtension extension() {
        if (extension == null)
            extension = (QuarkusPluginExtension) getProject().getExtensions().findByName("quarkus");
        return extension;
    }
}
