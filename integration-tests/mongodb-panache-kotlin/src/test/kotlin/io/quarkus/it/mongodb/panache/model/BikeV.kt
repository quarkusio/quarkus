package io.quarkus.it.mongodb.panache.model

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.common.Version
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId

@MongoEntity
class BikeV @BsonCreator constructor(
    @BsonProperty("modelYear") var modelYear: Int?,
    @BsonProperty("version") @Version
    var version: Long?
) : PanacheMongoEntity() {

    constructor(id: ObjectId?, modelYear: Int?, version: Long?) : this(modelYear, version) {
        this.id = id
    }
}
