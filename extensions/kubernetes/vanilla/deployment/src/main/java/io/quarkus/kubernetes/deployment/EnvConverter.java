
package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnvConverter {

    public static String convertName(String name) {
        return name != null ? name.toUpperCase().replace('-', '_').replace('.', '_').replace('/', '_') : null;
    }

    static Optional<String> extractSecretPrefix(String secret, Map<String, EnvVarPrefixConfig> mappingWithPrefix) {
        return mappingWithPrefix.entrySet().stream()
                .filter(m -> m.getValue().hasPrefixForSecret(secret))
                .findFirst().map(Map.Entry::getKey);
    }

    static Optional<String> extractConfigmapPrefix(String configmap,
            Map<String, EnvVarPrefixConfig> mappingWithPrefix) {
        return mappingWithPrefix.entrySet().stream()
                .filter(m -> m.getValue().hasPrefixForConfigmap(configmap))
                .findFirst().map(Map.Entry::getKey);
    }

    static Map<String, EnvVarPrefixConfig> collectPrefixes(EnvVarsConfig e) {
        return e.prefixes().entrySet().stream()
                .filter(p -> p.getValue().anyPresent() && p.getKey() != null && !p.getKey().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
