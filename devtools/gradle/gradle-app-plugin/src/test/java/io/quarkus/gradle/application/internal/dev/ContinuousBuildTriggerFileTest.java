package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContinuousBuildTriggerFileTest {

    @TempDir
    Path directory;

    @Test
    void initializesWithoutOverwritingAndPublishesMonotonicValues() throws Exception {
        Path trigger = directory.resolve("trigger");
        ContinuousBuildTriggerFile.initialize(trigger);
        assertThat(trigger).hasContent("epoch=initializer\ngeneration=0\n");

        String epoch = UUID.randomUUID().toString();
        ContinuousBuildTriggerFile owner = new ContinuousBuildTriggerFile(trigger, epoch);
        assertThat(owner.publishNext()).isTrue();
        assertThat(trigger).hasContent("epoch=" + epoch + "\ngeneration=1\n");
        assertThat(owner.publish(1)).isFalse();
        assertThat(owner.publish(4)).isTrue();
        assertThat(trigger).hasContent("epoch=" + epoch + "\ngeneration=4\n");

        ContinuousBuildTriggerFile.initialize(trigger);
        assertThat(trigger).hasContent("epoch=" + epoch + "\ngeneration=4\n");
        owner.close();
        assertThat(owner.publish(5)).isFalse();
        assertThat(trigger).hasContent("epoch=" + epoch + "\ngeneration=4\n");
    }

    @Test
    void failedReplacementDoesNotAdvanceGenerationAndCleansTemporaryFile() throws Exception {
        Path trigger = Files.createDirectory(directory.resolve("trigger"));
        String epoch = UUID.randomUUID().toString();
        ContinuousBuildTriggerFile owner = new ContinuousBuildTriggerFile(trigger, epoch);

        assertThatThrownBy(owner::publishNext).isInstanceOf(IOException.class);
        try (var files = Files.list(directory)) {
            assertThat(files).allMatch(path -> path.equals(trigger));
        }

        Files.delete(trigger);
        assertThat(owner.publishNext()).isTrue();
        assertThat(trigger).hasContent("epoch=" + epoch + "\ngeneration=1\n");
    }

    @Test
    void validatesEpochGenerationAndOverflow() throws Exception {
        Path trigger = directory.resolve("trigger");

        assertThatThrownBy(() -> new ContinuousBuildTriggerFile(trigger, "not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContinuousBuildTriggerFile(trigger, UUID.randomUUID().toString(), -1))
                .isInstanceOf(IllegalArgumentException.class);
        ContinuousBuildTriggerFile exhausted = new ContinuousBuildTriggerFile(
                trigger, UUID.randomUUID().toString(), Long.MAX_VALUE);
        assertThatThrownBy(exhausted::publishNext)
                .isInstanceOf(ArithmeticException.class);
        exhausted.close();
        assertThat(exhausted.publishNext()).isFalse();
    }
}
