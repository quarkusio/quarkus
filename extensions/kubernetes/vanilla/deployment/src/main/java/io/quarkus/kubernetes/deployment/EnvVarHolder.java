package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.EnvConverter.collectPrefixes;
import static io.quarkus.kubernetes.deployment.EnvConverter.extractConfigmapPrefix;
import static io.quarkus.kubernetes.deployment.EnvConverter.extractSecretPrefix;

import java.util.Collection;
import java.util.Map;

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
     * Specifies which the name of the platform this EnvVarHolder targets. This name, when needed, is used by dekorate
     * to generate the descriptor associated with the targeted deployment platform.
     *
     * @return the name of the targeted platform e.g. {@link Constants#KUBERNETES}
     */
    default String targetPlatformName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts the environment variable configuration held by this EnvVarHolder (as returned by {@link #env()}) into a
     * collection of associated {@link KubernetesEnvBuildItem}.
     *
     * @return a collection of {@link KubernetesEnvBuildItem} corresponding to the environment variable configurations
     */
    default Collection<KubernetesEnvBuildItem> convertToBuildItems() {
        EnvVarValidator validator = new EnvVarValidator();

        final String target = targetPlatformName();
        final EnvVarsConfig c = env();
        Map<String, EnvVarPrefixConfig> prefixMap = collectPrefixes(c);

        // Sort keys alphabetically to ensure deterministic ordering
        c.vars().keySet().stream().sorted()
                .forEach(k -> validator.process(
                        KubernetesEnvBuildItem.createSimpleVar(k, c.vars().get(k).value().orElse(""), target)));
        c.fields().keySet().stream().sorted()
                .forEach(k -> validator.process(KubernetesEnvBuildItem.createFromField(k, c.fields().get(k), target)));
        c.configmaps()
                .ifPresent(cl -> cl.stream()
                        .forEach(cm -> validator.process(KubernetesEnvBuildItem.createFromConfigMap(cm,
                                target, extractConfigmapPrefix(cm, prefixMap).orElse(null)))));
        c.secrets().ifPresent(sl -> sl.stream()
                .forEach(s -> validator.process(KubernetesEnvBuildItem.createFromSecret(s,
                        target, extractSecretPrefix(s, prefixMap).orElse(null)))));
        c.optionalConfigmaps()
                .ifPresent(cl -> cl.stream()
                        .forEach(cm -> validator.process(KubernetesEnvBuildItem.createFromConfigMap(cm,
                                target, extractConfigmapPrefix(cm, prefixMap).orElse(null), true))));
        c.optionalSecrets().ifPresent(sl -> sl.stream()
                .forEach(s -> validator.process(KubernetesEnvBuildItem.createFromSecret(s,
                        target, extractSecretPrefix(s, prefixMap).orElse(null), true))));
        c.mapping().keySet().stream().sorted()
                .forEach(varName -> {
                    var config = c.mapping().get(varName);
                    validator.process(KubernetesEnvBuildItem.createFromResourceKey(varName, config.withKey(),
                            config.fromSecret().orElse(null), config.fromConfigmap().orElse(null), target));
                });

        return validator.getBuildItems();
    }
}
