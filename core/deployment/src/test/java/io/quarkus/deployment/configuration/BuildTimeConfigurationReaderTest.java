package io.quarkus.deployment.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

class BuildTimeConfigurationReaderTest {

    @Test
    void shouldNotThrowWhenIsPartOfQuarkusCoreRuntime() {
        Assertions.assertDoesNotThrow(() -> {
            BuildTimeConfigurationReader.checkIfIsOnDeploymentModule(
                    io.quarkus.runtime.ConfigConfig.class, ConfigPhase.RUN_TIME);

        });
    }

    @Test
    void shouldThrowExceptionWhenIsRunTimeAndIsOnDeploymentModule() {

        QuarkusClassLoader classLoader = QuarkusClassLoader
                .builder("QuarkusClassLoader :: Testing", getClass().getClassLoader(), false).build();
        Thread.currentThread().setContextClassLoader(classLoader);

        String message = assertThrows(RuntimeException.class, () -> {
            BuildTimeConfigurationReader.checkIfIsOnDeploymentModule(
                    RunTimeConfig.class, ConfigPhase.RUN_TIME);
        }).getMessage();

        assertThat(message).contains(
                "Configuration classes with ConfigPhase.RUN_TIME or ConfigPhase.BUILD_AND_RUNTIME_FIXED");
    }

    @Test
    void shouldThrowExceptionWhenIsBuildTimeAndRunTimeFixedAndIsOnDeploymentModule() {

        QuarkusClassLoader classLoader = QuarkusClassLoader
                .builder("QuarkusClassLoader :: Testing", getClass().getClassLoader(), false).build();
        Thread.currentThread().setContextClassLoader(classLoader);

        String message = assertThrows(RuntimeException.class, () -> {
            BuildTimeConfigurationReader.checkIfIsOnDeploymentModule(
                    BuildAndRunTimeFixed.class, ConfigPhase.BUILD_AND_RUN_TIME_FIXED);
        }).getMessage();

        assertThat(message).contains(
                "Configuration classes with ConfigPhase.RUN_TIME or ConfigPhase.BUILD_AND_RUNTIME_FIXED");
    }

    @Test
    void shouldNotThrowWhenIsBuildTimePhase() {
        Assertions.assertDoesNotThrow(() -> {
            BuildTimeConfigurationReader.checkIfIsOnDeploymentModule(
                    BuildTimeConfig.class, ConfigPhase.BUILD_TIME);
        });
    }

    @ConfigMapping
    @ConfigRoot(phase = ConfigPhase.RUN_TIME)
    static class RunTimeConfig {
    }

    @ConfigMapping
    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class BuildTimeConfig {
    }

    @ConfigMapping
    @ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    static class BuildAndRunTimeFixed {
    }
}
