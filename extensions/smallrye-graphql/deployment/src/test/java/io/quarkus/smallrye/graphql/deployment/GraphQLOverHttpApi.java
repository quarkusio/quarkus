package io.quarkus.smallrye.graphql.deployment;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Query;

/**
 * This is to test compatibility with the GraphQL over HTTP Spec
 * 
 * @see <a href="https://github.com/graphql/graphql-over-http/blob/main/spec/GraphQLOverHTTP.md">GraphQL over HTTP<a/>
 */
@GraphQLApi
public class GraphQLOverHttpApi {

    @Query
    public User getUser(@Id String id) {
        return new User(id, "Koos", "van der Merwe");
    }
}
