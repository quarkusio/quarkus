package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity
import org.bson.types.ObjectId

class LinkedEntity : PanacheMongoEntity() {
    companion object: PanacheMongoCompanion<LinkedEntity>

    var name: String? = null
    var myForeignId: ObjectId? = null
}