package io.quarkus.smallrye.graphql.client.runtime;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;

@Recorder
public class SmallRyeGraphQLClientRecorder {

    public <T> Supplier<T> typesafeClientSupplier(Class<T> targetClassName) {
        return () -> {
            TypesafeGraphQLClientBuilder builder = TypesafeGraphQLClientBuilder.newBuilder();
            return builder.build(targetClassName);
        };
    }
}
