package io.quarkus.it.mongodb.panache.model

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.common.Version
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty

@MongoEntity
class CarVEntity @BsonCreator constructor(
    @BsonId override var id: String,
    @BsonProperty("modelYear") override var modelYear: Int?,
    @BsonProperty("version") @Version
    var version: Long?
) : VehicleEntity(id, modelYear)
