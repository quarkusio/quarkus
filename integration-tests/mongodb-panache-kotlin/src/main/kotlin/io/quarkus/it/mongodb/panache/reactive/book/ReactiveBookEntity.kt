package io.quarkus.it.mongodb.panache.reactive.book

import io.quarkus.it.mongodb.panache.book.BookDetail
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty
import java.time.LocalDate
import java.util.ArrayList
import javax.json.bind.annotation.JsonbDateFormat

@MongoEntity(collection = "TheBookEntity", clientName = "cl2")
class ReactiveBookEntity : ReactivePanacheMongoEntity() {
    companion object: ReactivePanacheMongoCompanion<ReactiveBookEntity>

    @BsonProperty("bookTitle")
    var title: String? = null
        private set
    var author: String? = null
        private set

    @BsonIgnore
    var transientDescription: String? = null

    @JsonbDateFormat("yyyy-MM-dd")
    var creationDate: LocalDate? = null
    var categories: List<String> = ArrayList()
        private set
    var details: BookDetail? = null
        private set

    fun setTitle(title: String?): ReactiveBookEntity {
        this.title = title
        return this
    }

    fun setAuthor(author: String?): ReactiveBookEntity {
        this.author = author
        return this
    }

    fun setCategories(categories: List<String>): ReactiveBookEntity {
        this.categories = categories
        return this
    }

    fun setDetails(details: BookDetail?): ReactiveBookEntity {
        this.details = details
        return this
    }

}