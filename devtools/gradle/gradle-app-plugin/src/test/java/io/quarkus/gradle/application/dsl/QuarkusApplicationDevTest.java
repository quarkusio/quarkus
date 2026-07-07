package io.quarkus.gradle.application.dsl;

import static io.quarkus.gradle.testing.BaseGradleTest.canonicalPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;

class QuarkusApplicationDevTest {

    @TempDir
    Path projectDirectory;

    @Test
    void providesManagedLaunchConfiguration() {
        var project = ProjectBuilder.builder().withProjectDir(projectDirectory.toFile()).build();
        var dev = project.getObjects().newInstance(QuarkusApplicationDev.class);

        assertThat(canonicalPath(dev.getWorkingDirectory().get().getAsFile().toPath()))
                .isEqualTo(canonicalPath(projectDirectory));
        assertThat(dev.getEnvironmentVariables().get()).isEmpty();
        assertThat(dev.getDebug().isPresent()).isFalse();
        assertThat(dev.getDebugMode().isPresent()).isFalse();
        assertThat(dev.getDebugHost().isPresent()).isFalse();
        assertThat(dev.getDebugPort().isPresent()).isFalse();
        assertThat(dev.getSuspend().isPresent()).isFalse();
        assertThat(dev.getForceC2().isPresent()).isFalse();
        assertThat(dev.getExtensionJvmOptions().getDisableAll().get()).isFalse();
        assertThat(dev.getExtensionJvmOptions().getDisableFor().get()).isEmpty();

        dev.getEnvironmentVariables().put("APP_MODE", "local");
        dev.getDebug().set(true);
        dev.getDebugMode().set(QuarkusApplicationDevDebugMode.CONNECT);
        dev.getDebugHost().set("localhost");
        dev.getDebugPort().set(5005);
        dev.getSuspend().set(false);
        dev.getForceC2().set(false);
        dev.extensionJvmOptions(options -> {
            options.getDisableAll().set(true);
            options.getDisableFor().add("org.acme:acme-extension");
        });

        assertThat(dev.getEnvironmentVariables().get()).containsEntry("APP_MODE", "local");
        assertThat(dev.getDebug().get()).isTrue();
        assertThat(dev.getDebugMode().get()).isEqualTo(QuarkusApplicationDevDebugMode.CONNECT);
        assertThat(dev.getDebugHost().get()).isEqualTo("localhost");
        assertThat(dev.getDebugPort().get()).isEqualTo(5005);
        assertThat(dev.getSuspend().get()).isFalse();
        assertThat(dev.getForceC2().get()).isFalse();
        assertThat(dev.getExtensionJvmOptions().getDisableAll().get()).isTrue();
        assertThat(dev.getExtensionJvmOptions().getDisableFor().get())
                .containsExactly("org.acme:acme-extension");
    }
}
