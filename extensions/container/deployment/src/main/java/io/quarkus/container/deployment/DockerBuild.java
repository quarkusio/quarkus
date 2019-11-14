
package io.quarkus.container.deployment;

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
        if (containerConfig.build) {
            try {
                return ExecUtil.exec("docker", "version");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
