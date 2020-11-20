package io.quarkus.it.mongodb.panache.person

import org.bson.codecs.pojo.annotations.BsonId

class Person {
    @BsonId
    var id: Long? = null
    var firstname: String? = null
    var lastname: String? = null
    var status: Status = Status.ALIVE
}