package io.quarkus.bootstrap.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevModeMediatorTest {

    @TempDir
    Path directory;

    @Test
    void canceledDeletionDoesNotRemoveARecreatedFileOnTheNextRestart() throws Exception {
        Path dependency = Files.createFile(directory.resolve("dependency.jar"));
        DevModeMediator.scheduleDelete(List.of(dependency));
        DevModeMediator.cancelDelete(dependency);

        DevModeMediator.deleteScheduledFiles();

        assertThat(dependency).exists();
    }
}
