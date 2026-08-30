package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuarkusApplicationDevReplayTriggerTaskTest {

    @TempDir
    Path directory;

    @Test
    void initializesMissingTriggerWithoutOverwritingDeploymentValue() throws Exception {
        var project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        var task = project.getTasks().create("initializeReplay",
                QuarkusApplicationDevReplayTriggerTask.class);
        Path trigger = directory.resolve("build/replay.trigger");
        task.getTriggerFile().set(trigger.toFile());

        task.initialize();
        assertThat(trigger).hasContent("epoch=initializer\ngeneration=0\n");

        Files.writeString(trigger, "epoch=deployment\ngeneration=2\n", StandardCharsets.UTF_8);
        task.initialize();
        assertThat(trigger).hasContent("epoch=deployment\ngeneration=2\n");
    }
}
