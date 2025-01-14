package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.Env;
import io.smallrye.config.WithDefault;

public interface ContainerConfig extends EnvVarHolder {
    /**
     * The container image.
     */
    Optional<String> image();

    /**
     * Working directory.
     */
    Optional<String> workingDir();

    /**
     * The commands
     */
    Optional<List<String>> command();

    /**
     * The arguments
     *
     * @return The arguments.
     */
    Optional<List<String>> arguments();

    /**
     * The service account.
     */
    Optional<String> serviceAccount();

    /**
     * The host under which the application is going to be exposed.
     *
     */
    Optional<String> host();

    /**
     * The application ports.
     */
    Map<String, PortConfig> ports();

    /**
     * Image pull policy.
     */
    @WithDefault("always")
    ImagePullPolicy imagePullPolicy();

    /**
     * The image pull secrets.
     */
    Optional<List<String>> imagePullSecrets();

    /**
     * The liveness probe.
     */
    ProbeConfig livenessProbe();

    /**
     * The readiness probe.
     */
    ProbeConfig readinessProbe();

    /**
     * Volume mounts.
     */
    Map<String, MountConfig> mounts();

    /**
     * Resources requirements
     */
    ResourcesConfig resources();

    @Override
    default String targetPlatformName() {
        // ContainerConfig doesn't need a deployment target since it doesn't need to process KubernetesEnvBuildItem apart to
        // convert them to Env instances once processed by the EnvVarValidator. This trick is used to be able to reuse the
        // logic supporting old and new env var syntax.
        return null;
    }

    default Collection<Env> convertToEnvs() {
        return convertToBuildItems().stream()
                .map(kebi -> new Env(EnvConverter.convertName(kebi.getName()), kebi.getValue(), kebi.getSecret(),
                        kebi.getConfigMap(), kebi.getField(), null, kebi.getPrefix()))
                .collect(Collectors.toList());
    }
}
