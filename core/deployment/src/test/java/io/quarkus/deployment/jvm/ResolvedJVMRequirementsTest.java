package io.quarkus.deployment.jvm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

public class ResolvedJVMRequirementsTest {

    @Test
    public void addOpensAreRenderedAsJvmArguments() throws BuildException {
        ResolvedJVMRequirements requirements = new ResolvedJVMRequirements(
                List.of(new ModuleOpenBuildItem("java.base", "org.jboss.threads", "java.lang"),
                        new ModuleOpenBuildItem("java.base", "io.netty.common", "java.nio", "java.io")),
                List.of());

        assertThat(requirements.renderAsJvmArguments()).containsExactly(
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED");
    }

    @Test
    public void nativeAccessIsRenderedAsJvmArgument() throws BuildException {
        ResolvedJVMRequirements requirements = new ResolvedJVMRequirements(
                List.of(new ModuleOpenBuildItem("java.base", "org.jboss.threads", "java.lang")),
                List.of(new ModuleEnableNativeAccessBuildItem("io.netty.common")));

        assertThat(requirements.renderAsJvmArguments()).containsExactly(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED");
    }

    @Test
    public void noRequirementsRenderNoJvmArguments() throws BuildException {
        ResolvedJVMRequirements requirements = new ResolvedJVMRequirements(List.of(), List.of());

        assertThat(requirements.renderAsJvmArguments()).isEmpty();
    }
}
