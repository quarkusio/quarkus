package org.acme

import org.eclipse.microprofile.graphql.DefaultValue
import org.eclipse.microprofile.graphql.Description
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Query

@GraphQLApi
class HelloGraphQLResource {

    @Query
    @Description("Say hello")
    String sayHello(@DefaultValue("World") String name) {
        "Hello ${name}"
    }
}