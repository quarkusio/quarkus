package io.quarkus.it.mongodb.panache.test

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity

class TestImperativeEntity : PanacheMongoEntity {
    companion object : PanacheMongoCompanion<TestImperativeEntity>

    lateinit var title: String
    lateinit var category: String
    lateinit var description: String
    var ctp = 1

    constructor() {}

    constructor(title: String, category: String, description: String) {
        this.title = title
        this.category = category
        this.description = description
    }
}
