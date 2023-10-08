package io.quarkus.deployment.dev.devservices;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a running container.
 *
 * @param containerInfo The container information.
 * @param env The container environment variables.
 */
public record RunningContainer(ContainerInfo containerInfo, Map<String, String> env) {

    public Optional<Integer> getPortMapping(int port) {
        return Arrays.stream(containerInfo.exposedPorts())
                .filter(p -> p.privatePort() != null && p.privatePort() == port)
                .map(ContainerInfo.ContainerPort::publicPort)
                .findFirst();
    }

    public Optional<String> tryGetEnv(String... keys) {
        for (String key : keys) {
            String value = env.get(key);
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
