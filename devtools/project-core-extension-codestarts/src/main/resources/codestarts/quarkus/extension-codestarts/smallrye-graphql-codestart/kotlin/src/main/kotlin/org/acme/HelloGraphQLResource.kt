package org.acme

import org.eclipse.microprofile.graphql.DefaultValue
import org.eclipse.microprofile.graphql.Description
import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.Query

@GraphQLApi
class HelloGraphQLResource {

    @Query
    @Description("Say hello")
    fun sayHello(@DefaultValue("World") name: String): String = "Hello $name"

}