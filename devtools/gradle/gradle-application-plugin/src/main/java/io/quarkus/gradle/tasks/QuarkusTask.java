package io.quarkus.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class QuarkusTask extends DefaultTask {

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        setDescription(description);
        setGroup("quarkus");
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    QuarkusPluginExtension extension() {
        if (extension == null) {
            extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        }
        return extension;
    }
}
