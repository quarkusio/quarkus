package io.quarkus.deployment;

import static io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE;

import java.util.List;

import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;

public class IsPodmanWorking extends IsContainerRuntimeWorking {
    public IsPodmanWorking() {
        this(false);
    }

    public IsPodmanWorking(boolean silent) {
        super(List.of(
                new TestContainersStrategy(silent),
                new DockerHostStrategy(),
                new PodmanBinaryStrategy()));
    }

    private static class PodmanBinaryStrategy implements Strategy {
        @Override
        public Result get() {
            if (ContainerRuntimeUtil.detectContainerRuntime(false, ContainerRuntime.PODMAN) != UNAVAILABLE) {
                return Result.AVAILABLE;
            } else {
                return Result.UNKNOWN;
            }
        }
    }
}
