package io.quarkus.it.mongodb.panache.test

import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity

class TestReactiveEntity : ReactivePanacheMongoEntity {
    companion object : ReactivePanacheMongoCompanion<TestReactiveEntity>

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
