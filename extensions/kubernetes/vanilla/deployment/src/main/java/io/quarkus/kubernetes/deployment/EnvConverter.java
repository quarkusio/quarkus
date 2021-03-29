
package io.quarkus.kubernetes.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.config.EnvBuilder;

public class EnvConverter {
    public static Env convert(Map.Entry<String, EnvConfig> e) {
        return convert(e.getValue()).withName(convertName(e.getKey())).build();
    }

    private static EnvBuilder convert(EnvConfig env) {
        EnvBuilder b = new EnvBuilder();
        env.name.ifPresent(b::withName);
        env.value.ifPresent(b::withValue);
        env.secret.ifPresent(b::withSecret);
        env.configmap.ifPresent(b::withConfigmap);
        env.field.ifPresent(b::withField);
        return b;
    }

    public static List<Env> convert(EnvVarsConfig e) {
        List<Env> envs = new LinkedList<>();
        e.secrets.ifPresent(sl -> sl.forEach(s -> envs.add(new EnvBuilder().withName(convertName(s)).withSecret(s).build())));
        e.configmaps
                .ifPresent(cl -> cl.forEach(c -> envs.add(new EnvBuilder().withName(convertName(c)).withConfigmap(c).build())));
        e.vars.forEach((k, v) -> envs.add(new EnvBuilder().withName(convertName(k)).withValue(v).build()));
        e.fields.forEach((k, v) -> {
            // env vars from fields need to have their name set in addition to their field field :)
            final String field = convertName(k);
            envs.add(new EnvBuilder().withName(field).withField(field).withValue(v).build());
        });
        e.mapping.forEach(
                (k, v) -> envs.add(new EnvBuilder().withName(convertName(k)).withSecret(v.fromSecret.orElse(null))
                        .withConfigmap(v.fromConfigmap.orElse(null)).withValue(v.withKey).build()));
        return envs;
    }

    public static String convertName(String name) {
        return name != null ? name.toUpperCase().replace('-', '_').replace('.', '_').replace('/', '_') : null;
    }
}
