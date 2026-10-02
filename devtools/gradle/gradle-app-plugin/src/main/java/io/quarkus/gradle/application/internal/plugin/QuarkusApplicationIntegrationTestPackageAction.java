package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

final class QuarkusApplicationIntegrationTestPackageAction implements Action<Task> {

    private final Provider<Directory> metadataDirectory;

    QuarkusApplicationIntegrationTestPackageAction(Provider<Directory> metadataDirectory) {
        this.metadataDirectory = metadataDirectory;
    }

    @Override
    public void execute(Task task) {
        Test test = (Test) task;
        test.getSystemProperties().put("build.output.directory", metadataDirectory.get().getAsFile().getAbsolutePath());
        QuarkusApplicationIntegrationTestLaunchDefaults.configure(test);
    }
}
