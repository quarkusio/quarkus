package io.quarkus.gradle.application.internal.plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

final class QuarkusApplicationStartupArchiveCleanupAction implements Action<Task> {

    private final Provider<Directory> trainingDirectory;

    QuarkusApplicationStartupArchiveCleanupAction(Provider<Directory> trainingDirectory) {
        this.trainingDirectory = trainingDirectory;
    }

    @Override
    public void execute(Task task) {
        Path directory = trainingDirectory.get().getAsFile().toPath();
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clean startup-archive training directory " + directory, e);
        }
    }
}
