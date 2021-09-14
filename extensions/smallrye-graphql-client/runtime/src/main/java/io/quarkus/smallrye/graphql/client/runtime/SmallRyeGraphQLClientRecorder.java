package io.quarkus.smallrye.graphql.client.runtime;

import java.util.Map;
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

    /**
     * Translates quarkus.* configuration properties to system properties understood by SmallRye GraphQL.
     */
    public void translateClientConfiguration(GraphQLClientsConfig clientsConfig,
            Map<String, String> shortNamesToQualifiedNames) {
        for (Map.Entry<String, GraphQLClientConfig> client : clientsConfig.clients.entrySet()) {
            String configKey = client.getKey();
            // the config key can be a short class name, in which case we try to translate it to a fully qualified name
            // and use the FQ name in the set property, because that's what SmallRye GraphQL understands
            String clientName = shortNamesToQualifiedNames.getOrDefault(configKey, configKey);
            GraphQLClientConfig config = client.getValue();
            System.setProperty(clientName + "/mp-graphql/url", config.url);
            for (Map.Entry<String, String> header : config.headers.entrySet()) {
                System.setProperty(clientName + "/mp-graphql/header/" + header.getKey(), header.getValue());
            }
        }
    }
}
