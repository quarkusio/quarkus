package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.EnvConverter.collectPrefixes;
import static io.quarkus.kubernetes.deployment.EnvConverter.extractConfigmapPrefix;
import static io.quarkus.kubernetes.deployment.EnvConverter.extractSecretPrefix;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

/**
 * Common interface for configuration entities holding environment variables meant to be injected into containers.
 */
public interface EnvVarHolder {
    /**
     * Retrieves the definition of environment variables to add to the application's container.
     */
    EnvVarsConfig env();

    /**
     * @deprecated use {@link #env()} instead
     */
    @Deprecated
    Map<String, EnvConfig> envVars();

    /**
     * Specifies which the name of the platform this EnvVarHolder targets. This name, when needed, is used by dekorate
     * to generate the descriptor associated with the targeted deployment platform.
     *
     * @return the name of the targeted platform e.g. {@link Constants#KUBERNETES}
     */
    default String targetPlatformName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts the environment variable configuration held by this EnvVarHolder (as returned by {@link #env()} and
     * {@link #envVars()}) into a collection of associated {@link KubernetesEnvBuildItem}.
     *
     * @return a collection of {@link KubernetesEnvBuildItem} corresponding to the environment variable configurations
     */
    default Collection<KubernetesEnvBuildItem> convertToBuildItems() {
        EnvVarValidator validator = new EnvVarValidator();

        // first process old-style configuration, this relies on each configuration having a name
        String target = targetPlatformName();
        envVars().forEach((key, envConfig) -> {
            validator.process(key, envConfig.value(), envConfig.secret(), envConfig.configmap(), envConfig.field(), target,
                    Optional.empty(), true);
        });

        // override old-style with newer versions if present
        final EnvVarsConfig c = env();
        Map<String, EnvVarPrefixConfig> prefixMap = collectPrefixes(c);

        c.vars().forEach((k, v) -> validator.process(KubernetesEnvBuildItem.createSimpleVar(k, v.value().orElse(""), target)));
        c.fields().forEach((k, v) -> validator.process(KubernetesEnvBuildItem.createFromField(k, v, target)));
        c.configmaps()
                .ifPresent(
                        cl -> cl.forEach(cm -> validator.process(KubernetesEnvBuildItem.createFromConfigMap(cm,
                                target, extractConfigmapPrefix(cm, prefixMap).orElse(null)))));
        c.secrets().ifPresent(sl -> sl.forEach(s -> validator.process(KubernetesEnvBuildItem.createFromSecret(s,
                target, extractSecretPrefix(s, prefixMap).orElse(null)))));
        c.mapping().forEach(
                (varName, config) -> validator.process(KubernetesEnvBuildItem.createFromResourceKey(varName, config.withKey(),
                        config.fromSecret().orElse(null), config.fromConfigmap().orElse(null), target)));

        return validator.getBuildItems();
    }
}
