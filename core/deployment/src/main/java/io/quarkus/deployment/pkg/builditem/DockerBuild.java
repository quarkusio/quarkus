
package io.quarkus.deployment.pkg.builditem;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.ContainerConfig;
import io.quarkus.deployment.util.ExecUtil;

public class DockerBuild implements BooleanSupplier {

    private final ContainerConfig containerConfig;

    DockerBuild(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerConfig.build && ExecUtil.exec("docker", "version");
    }
}
