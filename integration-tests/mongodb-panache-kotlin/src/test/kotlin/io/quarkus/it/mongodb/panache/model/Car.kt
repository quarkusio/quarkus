package io.quarkus.it.mongodb.panache.model

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty

@MongoEntity
data class Car @BsonCreator constructor(
    @BsonId override var id: String,
    @BsonProperty("modelYear") override var modelYear: Int?
) : Vehicle(id, modelYear)
