package io.quarkus.smallrye.graphql.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.graphql.schema.model.Schema;

final class SmallRyeGraphQLSchemaBuildItem extends SimpleBuildItem {

    private final Schema schema;

    SmallRyeGraphQLSchemaBuildItem(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }
}
