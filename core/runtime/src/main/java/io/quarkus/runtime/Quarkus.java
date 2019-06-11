package io.quarkus.runtime;

import java.lang.reflect.Method;

/**
 * The main entry point class, calling main allows you to bootstrap quarkus
 * <p>
 * Note that at native image generation time this is replaced by {@link io.quarkus.runtime.graal.QuarkusReplacement}
 * which will avoid the need for reflection.
 * <p>
 * TODO: how do we deal with static init
 */
public class Quarkus {

    public static void main(String... args) throws Exception {
        Class main = Class.forName("io.quarkus.runner.GeneratedMain");
        Method mainMethod = main.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }
}
