
package io.quarkus.kubernetes.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.config.EnvBuilder;

public class EnvConverter {

    public static Env convert(Map.Entry<String, EnvConfig> e) {
        return convert(e.getValue()).withName(convertName(e.getKey())).build();
    }

    private static EnvBuilder convert(EnvConfig env) {
        EnvBuilder b = new EnvBuilder();
        env.name().ifPresent(b::withName);
        env.value().ifPresent(b::withValue);
        env.secret().ifPresent(b::withSecret);
        env.configmap().ifPresent(b::withConfigmap);
        env.field().ifPresent(b::withField);
        return b;
    }

    public static List<Env> convert(EnvVarsConfig e) {
        List<Env> envs = new LinkedList<>();

        Map<String, EnvVarPrefixConfig> prefixMap = collectPrefixes(e);

        e.secrets().ifPresent(sl -> sl
                .forEach(s -> envs.add(new EnvBuilder().withName(convertName(s)).withSecret(s)
                        .withPrefix(extractSecretPrefix(s, prefixMap).orElse(null)).build())));
        e.configmaps()
                .ifPresent(cl -> cl.forEach(c -> envs
                        .add(new EnvBuilder().withName(convertName(c)).withConfigmap(c)
                                .withPrefix(extractConfigmapPrefix(c, prefixMap).orElse(null)).build())));
        e.vars().forEach((k, v) -> envs.add(new EnvBuilder().withName(convertName(k)).withValue(v.value().orElse("")).build()));
        e.fields().forEach((k, v) -> {
            // env vars from fields need to have their name set in addition to their field field :)
            final String field = convertName(k);
            envs.add(new EnvBuilder().withName(field).withField(field).withValue(v).build());
        });
        e.mapping().forEach(
                (k, v) -> envs.add(new EnvBuilder().withName(convertName(k)).withSecret(v.fromSecret().orElse(null))
                        .withConfigmap(v.fromConfigmap().orElse(null)).withValue(v.withKey()).build()));
        return envs;
    }

    public static String convertName(String name) {
        return name != null ? name.toUpperCase().replace('-', '_').replace('.', '_').replace('/', '_') : null;
    }

    public static Optional<String> extractSecretPrefix(String secret, Map<String, EnvVarPrefixConfig> mappingWithPrefix) {
        return mappingWithPrefix.entrySet().stream()
                .filter(m -> m.getValue().hasPrefixForSecret(secret))
                .findFirst().map(Map.Entry::getKey);
    }

    public static Optional<String> extractConfigmapPrefix(String configmap,
            Map<String, EnvVarPrefixConfig> mappingWithPrefix) {
        return mappingWithPrefix.entrySet().stream()
                .filter(m -> m.getValue().hasPrefixForConfigmap(configmap))
                .findFirst().map(Map.Entry::getKey);
    }

    public static Map<String, EnvVarPrefixConfig> collectPrefixes(EnvVarsConfig e) {
        return e.prefixes().entrySet().stream()
                .filter(p -> p.getValue().anyPresent() && p.getKey() != null && !p.getKey().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
