
package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.Env;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ContainerConfig implements EnvVarHolder {

    /**
     * The container image.
     */
    @ConfigItem
    Optional<String> image;

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
    @ConfigItem(defaultValue = "Always")
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
    ProbeConfig livenessProbe;

    /**
     * The readiness probe.
     */
    @ConfigItem
    ProbeConfig readinessProbe;

    /**
     * Volume mounts.
     */
    @ConfigItem
    Map<String, MountConfig> mounts;

    /**
     * Resources requirements
     */
    @ConfigItem
    ResourcesConfig resources;

    /**
     * Environment variables to add to all containers using the old syntax.
     *
     * @deprecated Use {@link #env} instead using the new syntax as follows:
     *             <ul>
     *             <li>{@code quarkus.kubernetes.env-vars.foo.field=fieldName} becomes
     *             {@code quarkus.kubernetes.env.fields.foo=fieldName}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.envvar.value=value} becomes
     *             {@code quarkus.kubernetes.env.vars.envvar=value}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.bar.configmap=configName} becomes
     *             {@code quarkus.kubernetes.env.configmaps=configName}</li>
     *             <li>{@code quarkus.kubernetes.env-vars.baz.secret=secretName} becomes
     *             {@code quarkus.kubernetes.env.secrets=secretName}</li>
     *             </ul>
     */
    @ConfigItem
    @Deprecated
    Map<String, EnvConfig> envVars;

    /**
     * Environment variables to add to all containers.
     */
    @ConfigItem
    EnvVarsConfig env;

    /**
     * @deprecated use {@link #getEnv()} instead
     */
    @Deprecated
    public Map<String, EnvConfig> getEnvVars() {
        return envVars;
    }

    public EnvVarsConfig getEnv() {
        return env;
    }

    @Override
    public String getTargetPlatformName() {
        // ContainerConfig doesn't need a deployment target since it doesn't need to process KubernetesEnvBuildItem apart to
        // convert them to Env instances once processed by the EnvVarValidator. This trick is used to be able to reuse the
        // logic supporting old and new env var syntax.
        return null;
    }

    public Collection<Env> convertToEnvs() {
        return convertToBuildItems().stream()
                .map(kebi -> new Env(EnvConverter.convertName(kebi.getName()), kebi.getValue(), kebi.getSecret(),
                        kebi.getConfigMap(), kebi.getField(), null))
                .collect(Collectors.toList());
    }

}
