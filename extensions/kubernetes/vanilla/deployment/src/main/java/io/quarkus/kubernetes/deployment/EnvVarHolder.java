package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.Map;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

/**
 * Common interface for configuration entities holding environment variables meant to be injected into containers.
 */
public interface EnvVarHolder {
    /**
     * Retrieves the definition of environment variables to add to the application's container.
     *
     * @return the associated {@link EnvVarsConfig} holding the definition of which environment variables to add
     */
    EnvVarsConfig getEnv();

    /**
     * @deprecated use {@link #getEnv()} instead
     */
    @Deprecated
    Map<String, EnvConfig> getEnvVars();

    /**
     * Specifies which the name of the platform this EnvVarHolder targets. This name, when needed, is used by dekorate to
     * generate the descriptor associated with the targeted deployment platform.
     *
     * @return the name of the targeted platform e.g. {@link Constants#KUBERNETES}
     */
    String getTargetPlatformName();

    /**
     * Converts the environment variable configuration held by this EnvVarHolder (as returned by {@link #getEnv()} and
     * {@link #getEnvVars()}) into a collection of associated {@link KubernetesEnvBuildItem}.
     *
     * @return a collection of {@link KubernetesEnvBuildItem} corresponding to the environment variable configurations
     */
    default Collection<KubernetesEnvBuildItem> convertToBuildItems() {
        final EnvVarValidator validator = new EnvVarValidator();

        // first process old-style configuration, this relies on each configuration having a name
        final String target = getTargetPlatformName();
        getEnvVars().forEach((key, envConfig) -> {
            validator.process(key, envConfig.value, envConfig.secret, envConfig.configmap, envConfig.field, target,
                    true);
        });

        // override old-style with newer versions if present
        final EnvVarsConfig c = getEnv();
        c.vars.forEach((k, v) -> validator.process(KubernetesEnvBuildItem.createSimpleVar(k, v, target)));
        c.fields.forEach((k, v) -> validator.process(KubernetesEnvBuildItem.createFromField(k, v, target)));
        c.configmaps
                .ifPresent(cl -> cl.forEach(cm -> validator.process(KubernetesEnvBuildItem.createFromConfigMap(cm, target))));
        c.secrets.ifPresent(sl -> sl.forEach(s -> validator.process(KubernetesEnvBuildItem.createFromSecret(s, target))));
        c.mapping.forEach(
                (varName, config) -> validator.process(KubernetesEnvBuildItem.createFromResourceKey(varName, config.withKey,
                        config.fromSecret.orElse(null), config.fromConfigmap.orElse(null), target)));

        return validator.getBuildItems();
    }
}
