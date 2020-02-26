
package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.regex.Pattern;

import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.config.EnvBuilder;

public class EnvConverter {

    public static Env convert(Map.Entry<String, EnvConfig> e) {
        return convert(e.getValue()).withName(e.getKey().toUpperCase().replaceAll(Pattern.quote("-"), "_")).build();
    }

    private static EnvBuilder convert(EnvConfig env) {
        EnvBuilder b = new EnvBuilder();
        env.name.ifPresent(v -> b.withName(v));
        env.value.ifPresent(v -> b.withValue(v));
        env.secret.ifPresent(v -> b.withSecret(v));
        env.configmap.ifPresent(v -> b.withConfigmap(v));
        env.field.ifPresent(v -> b.withField(v));
        return b;
    }
}
