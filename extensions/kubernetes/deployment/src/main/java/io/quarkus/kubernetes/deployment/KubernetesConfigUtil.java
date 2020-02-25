package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_TARGET;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.S2I;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.utils.Strings;

public class KubernetesConfigUtil {

    private static final String DEKORATE_PREFIX = "dekorate.";

    private static final Set<String> ALLOWED_GENERATORS = new HashSet<>(
            Arrays.asList("kubernetes", "openshift", "knative", "docker", "s2i"));
    private static final Set<String> IMAGE_GENERATORS = new HashSet<>(Arrays.asList("docker", "s2i"));

    public static List<String> getDeploymentTargets(Map<String, Object> map) {
        return Arrays.stream(map.getOrDefault(DEKORATE_PREFIX + DEPLOYMENT_TARGET, KUBERNETES).toString().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public static Optional<String> getDockerRegistry(Map<String, Object> map) {
        return IMAGE_GENERATORS.stream().map(g -> map.get(DEKORATE_PREFIX + g + ".registry")).filter(Objects::nonNull)
                .map(String::valueOf).findFirst();
    }

    public static Optional<String> getGroup(Map<String, Object> map) {
        return ALLOWED_GENERATORS.stream().map(g -> map.get(DEKORATE_PREFIX + g + ".group")).filter(Objects::nonNull)
                .map(String::valueOf).findFirst();
    }

    public static Optional<String> getName(Map<String, Object> map) {
        return ALLOWED_GENERATORS.stream().map(g -> map.get(DEKORATE_PREFIX + g + ".name")).filter(Objects::nonNull)
                .map(String::valueOf).findFirst();
    }

    /*
     * Collects configuration properties for Kubernetes. Reads all properties and
     * matches properties that match known Dekorate generators. These properties may
     * or may not be prefixed with `quarkus.` though the prefixed ones take
     * precedence.
     *
     * @return A map containing the properties.
     */
    public static Map<String, Object> toMap(PlatformConfiguration... platformConfigurations) {
        Config config = ConfigProvider.getConfig();
        Map<String, Object> result = new HashMap<>();

        // Most of quarkus prefixed properties are handled directly by the config items (KubernetesConfig, OpenshiftConfig, KnativeConfig)
        // We just need group, name & version parsed here, as we don't have decorators for these (low level properties).
        Map<String, Object> quarkusPrefixed = new HashMap<>();
        Arrays.stream(platformConfigurations).forEach(p -> {
            p.getGroup().ifPresent(g -> quarkusPrefixed.put(DEKORATE_PREFIX + p.getConfigName() + ".group", g));
            p.getName().ifPresent(n -> quarkusPrefixed.put(DEKORATE_PREFIX + p.getConfigName() + ".name", n));
            p.getVersion().ifPresent(v -> quarkusPrefixed.put(DEKORATE_PREFIX + p.getConfigName() + ".version", v));
        });

        Map<String, Object> unPrefixed = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(k -> ALLOWED_GENERATORS.contains(generatorName(k)))
                .filter(k -> config.getOptionalValue(k, String.class).isPresent())
                .collect(Collectors.toMap(k -> DEKORATE_PREFIX + k, k -> config.getValue(k, String.class)));

        result.putAll(unPrefixed);
        result.putAll(quarkusPrefixed);
        result.putAll(toS2iProperties(quarkusPrefixed));
        return result;
    }

    /**
     * Returns the name of the generators that can handle the specified key.
     *
     * @param key The key.
     * @return The generator name or null if the key format is unexpected.
     */
    private static String generatorName(String key) {
        if (Strings.isNullOrEmpty(key) || !key.contains(".")) {
            return null;
        }
        return key.substring(0, key.indexOf("."));
    }

    private static Map<String, Object> toS2iProperties(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((k, v) -> {
            if (k.contains(OPENSHIFT)) {
                result.put(k.replaceAll(OPENSHIFT, S2I), v);
            }
        });
        return result;
    }
}
