package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.Env;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
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

    private static boolean isEnvVar(KubernetesEnvBuildItem env) {
        final var type = env.getType();
        return type == KubernetesEnvBuildItem.EnvType.var || type == KubernetesEnvBuildItem.EnvType.keyFromConfigmap
                || type == KubernetesEnvBuildItem.EnvType.keyFromSecret;
    }

    private static boolean isEnvFrom(KubernetesEnvBuildItem env) {
        return !isEnvVar(env);
    }

    default Collection<EnvVar> getEnvVars() {
        return convertToBuildItems().stream()
                .filter(ContainerConfig::isEnvVar)
                .map(kebi -> {
                    final var sourceBuilder = new EnvVarSourceBuilder();
                    final var type = kebi.getType();
                    switch (type) {
                        case field -> sourceBuilder.withNewFieldRef(null, kebi.getField()).build();
                        case keyFromConfigmap ->
                            sourceBuilder.withNewConfigMapKeyRef(kebi.getValue(), kebi.getConfigMap(), false);
                        case keyFromSecret -> sourceBuilder.withNewSecretKeyRef(kebi.getValue(), kebi.getSecret(), false);
                    }
                    return new EnvVar(EnvConverter.convertName(kebi.getName()),
                            type == KubernetesEnvBuildItem.EnvType.var ? kebi.getValue() : null,
                            sourceBuilder.build());
                })
                .toList();
    }

    default Collection<EnvFromSource> getEnvFroms() {
        return convertToBuildItems().stream()
                .filter(ContainerConfig::isEnvFrom)
                .map(kebi -> {
                    EnvFromSourceBuilder envFromBuilder = new EnvFromSourceBuilder();
                    final var type = kebi.getType();
                    switch (type) {
                        case secret -> envFromBuilder.withNewSecretRef(kebi.getSecret(), null);
                        case configmap -> envFromBuilder.withNewConfigMapRef(kebi.getConfigMap(), null);
                    }
                    envFromBuilder.withPrefix(kebi.getPrefix());
                    return envFromBuilder.build();
                })
                .toList();
    }
}
