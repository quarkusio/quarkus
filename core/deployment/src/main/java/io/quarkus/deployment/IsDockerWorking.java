
package io.quarkus.deployment;

import static io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE;

import java.util.List;

import io.quarkus.deployment.util.ContainerRuntimeUtil;

public class IsDockerWorking extends IsContainerRuntimeWorking {
    public IsDockerWorking() {
        this(false);
    }

    public IsDockerWorking(boolean silent) {
        super(List.of(new TestContainersStrategy(silent), new DockerHostStrategy(), new DockerBinaryStrategy()));
    }

    private static class DockerBinaryStrategy implements Strategy {
        @Override
        public Result get() {
            if (ContainerRuntimeUtil.detectContainerRuntime(false) != UNAVAILABLE) {
                return Result.AVAILABLE;
            } else {
                return Result.UNKNOWN;
            }
        }

    }
}
