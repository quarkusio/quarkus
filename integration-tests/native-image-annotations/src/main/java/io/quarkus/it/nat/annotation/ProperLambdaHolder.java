package io.quarkus.it.nat.annotation;

import java.io.Serializable;
import java.util.function.Function;

public class ProperLambdaHolder {
    public static Function<String, String> getLambda() {
        return (Function<String, String> & Serializable) s -> s + "_PROPER";
    }
}
