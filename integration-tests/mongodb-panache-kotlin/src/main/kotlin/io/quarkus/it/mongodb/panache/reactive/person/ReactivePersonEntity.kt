package io.quarkus.it.mongodb.panache.reactive.person

import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase
import org.bson.codecs.pojo.annotations.BsonId

class ReactivePersonEntity : ReactivePanacheMongoEntityBase<ReactivePersonEntity>() {
    companion object: ReactivePanacheMongoCompanionBase<ReactivePersonEntity, Long>

    @BsonId
    var id: Long? = null
    var firstname: String? = null
    var lastname: String? = null
}