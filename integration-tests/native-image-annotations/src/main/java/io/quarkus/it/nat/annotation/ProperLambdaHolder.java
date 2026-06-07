package io.quarkus.it.nat.annotation;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.function.Function;

import io.quarkus.runtime.annotations.LambdaDescriptor;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = { SerializedLambda.class }, serialization = true, lambdaDescriptors = {
        @LambdaDescriptor(declaringClass = ProperLambdaHolder.class, declaringMethod = "getLambda", parameterTypes = {}, interfaces = {
                Function.class })
})
public class ProperLambdaHolder {
    public static Function<String, String> getLambda() {
        return (Function<String, String> & Serializable) s -> s + "_PROPER";
    }
}
