package org.acme

import org.eclipse.microprofile.graphql.GraphQLApi
import org.eclipse.microprofile.graphql.NonNull
import org.eclipse.microprofile.graphql.Query

@GraphQLApi
class GraphQLResource {

    @Query
    @NonNull
    fun getBananas(): List<@NonNull Banana> {
        return listOf(Banana("yellow", 5), Banana("green", 3))
    }
}

class Banana(
    val color: String,
    val size: Int,
)
