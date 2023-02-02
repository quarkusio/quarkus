package io.quarkus.smallrye.graphql.runtime.spi;

import static io.quarkus.smallrye.graphql.runtime.SmallRyeGraphQLScalars.Upload;

import graphql.schema.GraphQLSchema;
import io.smallrye.graphql.spi.EventingService;

public class QuarkusGraphQLEventService implements EventingService {
    @Override
    public String getConfigKey() {
        return null; // activate this service always regardless of the configuration
    }

    @Override
    public GraphQLSchema.Builder beforeSchemaBuild(GraphQLSchema.Builder builder) {
        return builder.additionalType(Upload);
    }

}
