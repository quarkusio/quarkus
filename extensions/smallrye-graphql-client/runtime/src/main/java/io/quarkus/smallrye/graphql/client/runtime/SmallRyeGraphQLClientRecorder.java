package io.quarkus.smallrye.graphql.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.graphql.client.impl.GraphQLClientsConfiguration;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import io.smallrye.graphql.client.vertx.VertxManager;
import io.vertx.core.Vertx;

@Recorder
public class SmallRyeGraphQLClientRecorder {

    public <T> Supplier<T> typesafeClientSupplier(Class<T> targetClassName) {
        return () -> {
            TypesafeGraphQLClientBuilder builder = TypesafeGraphQLClientBuilder.newBuilder();
            return builder.build(targetClassName);
        };
    }

    public void setTypesafeApiClasses(List<String> apiClassNames) {
        GraphQLClientsConfiguration.setSingleApplication(true);
        GraphQLClientsConfiguration configBean = GraphQLClientsConfiguration.getInstance();
        List<Class<?>> classes = apiClassNames.stream().map(className -> {
            try {
                return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        configBean.addTypesafeClientApis(classes);
    }

    public RuntimeValue<GraphQLClientSupport> clientSupport(Map<String, String> shortNamesToQualifiedNames,
            List<String> knownConfigKeys) {
        GraphQLClientSupport support = new GraphQLClientSupport();
        support.setShortNamesToQualifiedNamesMapping(shortNamesToQualifiedNames);
        support.setKnownConfigKeys(knownConfigKeys);
        return new RuntimeValue<>(support);
    }

    public void initializeConfigurationMergerBean() {
        GraphQLClientConfigurationMergerBean merger = Arc.container()
                .instance(GraphQLClientConfigurationMergerBean.class).get();
        merger.nothing();
    }

    public void setGlobalVertxInstance(Supplier<Vertx> vertx) {
        VertxManager.setFromGlobal(vertx.get());
    }

}
