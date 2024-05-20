package io.quarkus.deployment;

import static io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE;

import java.util.List;

import io.quarkus.deployment.util.ContainerRuntimeUtil;

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
            return (ContainerRuntimeUtil.detectContainerRuntime(false) != UNAVAILABLE) ? Result.AVAILABLE : Result.UNKNOWN;
        }
    }
}
