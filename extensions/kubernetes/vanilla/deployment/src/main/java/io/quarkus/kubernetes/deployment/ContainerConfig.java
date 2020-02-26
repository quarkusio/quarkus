
package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ContainerConfig {

    /**
     * The container image.
     */
    @ConfigItem
    Optional<String> image;

    /**
     * Environment variables to add to all containers.
     */
    @ConfigItem
    Map<String, EnvConfig> envVars;

    /**
     * Working directory.
     */
    @ConfigItem
    Optional<String> workingDir;

    /**
     * The commands
     */
    @ConfigItem
    Optional<List<String>> command;

    /**
     * The arguments
     *
     * @return The arguments.
     */
    @ConfigItem
    Optional<List<String>> arguments;

    /**
     * The service account.
     */
    @ConfigItem
    Optional<String> serviceAccount;

    /**
     * The host under which the application is going to be exposed.
     *
     */
    @ConfigItem
    Optional<String> host;

    /**
     * The application ports.
     */
    @ConfigItem
    Map<String, PortConfig> ports;

    /**
     * Image pull policy.
     */
    @ConfigItem(defaultValue = "IfNotPresent")
    ImagePullPolicy imagePullPolicy;

    /**
     * The image pull secret
     */
    @ConfigItem
    Optional<List<String>> imagePullSecrets;

    /**
     * The liveness probe.
     */
    @ConfigItem
    Optional<ProbeConfig> livenessProbe;

    /**
     * The readiness probe.
     */
    @ConfigItem
    Optional<ProbeConfig> readinessProbe;

    /**
     * Volume mounts.
     */
    @ConfigItem
    Map<String, MountConfig> mounts;

}
