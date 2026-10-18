
package io.quarkus.deployment;

import static io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE;

import java.util.List;

import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;

public class IsDockerWorking extends IsContainerRuntimeWorking {
    public IsDockerWorking() {
        this(false);
    }

    public IsDockerWorking(boolean silent) {
        super(List.of(new TestContainersStrategy(silent), new DockerHostStrategy(silent), new DockerBinaryStrategy(silent)));
    }

    private static class DockerBinaryStrategy implements Strategy {
        private final boolean silent;

        public DockerBinaryStrategy(boolean silent) {
            this.silent = silent;
        }

        @Override
        public Result get() {
            if (ContainerRuntimeUtil.detectContainerRuntime(false, silent,
                    ContainerRuntime.DOCKER, ContainerRuntime.PODMAN) != UNAVAILABLE) {
                return Result.AVAILABLE;
            } else {
                return Result.UNKNOWN;
            }
        }

    }
}
