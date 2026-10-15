package io.quarkus.modular.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ModularityStepsTest {

    @Test
    void jdkModulesAreRecognized() {
        assertThat(ModularitySteps.isJdkModule("java.base")).isTrue();
        assertThat(ModularitySteps.isJdkModule("java.sql")).isTrue();
        assertThat(ModularitySteps.isJdkModule("jdk.jfr")).isTrue();
        assertThat(ModularitySteps.isJdkModule("ibm.jvm")).isTrue();
    }

    @Test
    void moduleNamedLikeAJdkModuleIsNotOne() {
        assertThat(ModularitySteps.isJdkModule("java.money")).isFalse();
        assertThat(ModularitySteps.isJdkModule("java.xml.bind")).isFalse();
        assertThat(ModularitySteps.isJdkModule("java.validation")).isFalse();
    }
}
