package io.quarkus.gradle.application.internal.plugin;

import java.nio.file.Path;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

final class QuarkusApplicationIntegrationTestNativeAction implements Action<Task> {

    private final Provider<Directory> metadataDirectory;
    private final Provider<RegularFile> nativeResultFile;
    private final String suiteName;
    private final String buildName;

    QuarkusApplicationIntegrationTestNativeAction(Provider<Directory> metadataDirectory,
            Provider<RegularFile> nativeResultFile, String suiteName, String buildName) {
        this.metadataDirectory = metadataDirectory;
        this.nativeResultFile = nativeResultFile;
        this.suiteName = suiteName;
        this.buildName = buildName;
    }

    @Override
    public void execute(Task task) {
        Path resultFile = nativeResultFile.get().getAsFile().toPath();
        Path executablePath = QuarkusApplicationNativeExecutableResolver.resolve(resultFile, suiteName, buildName);
        Test test = (Test) task;
        test.getSystemProperties().put("build.output.directory", metadataDirectory.get().getAsFile().getAbsolutePath());
        test.getSystemProperties().put("native.image.path", executablePath.toAbsolutePath().toString());
        QuarkusApplicationIntegrationTestLaunchDefaults.configure(test);
    }
}
