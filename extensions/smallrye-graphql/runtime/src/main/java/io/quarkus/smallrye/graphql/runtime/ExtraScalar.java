package io.quarkus.smallrye.graphql.runtime;

public enum ExtraScalar {

    /**
     * The UUID GraphQL type, represented as a java.util.UUID
     */
    UUID,

    /**
     * The Object GraphQL type, represented as a general java.lang.Object
     */
    OBJECT,

    /**
     * The JSON GraphQL type, represented as a jakarta.json.JsonObject
     */
    JSON

}
