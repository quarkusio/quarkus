package io.quarkus.it.mongodb.panache.person

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase
import io.quarkus.panache.common.Sort
import org.bson.codecs.pojo.annotations.BsonId

class PersonEntity : PanacheMongoEntityBase() {
    @BsonId
    var id: Long? = null
    var firstname: String? = null
    var lastname: String? = null
    var status: Status = Status.ALIVE

    companion object : PanacheMongoCompanionBase<PersonEntity, Long> {
        fun findOrdered(): List<PersonEntity> {
            return findAll(Sort.by("lastname", "firstname")).list()
        }

    }
}