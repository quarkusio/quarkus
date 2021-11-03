package io.quarkus.it.mongodb.panache.book

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity
import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations.Companion.INSTANCE
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.LocalDate
import javax.json.bind.annotation.JsonbDateFormat

@MongoEntity(collection = "TheBookEntity", clientName = "cl2")
class BookEntity : PanacheMongoEntity() {
    companion object : PanacheMongoCompanion<BookEntity> {
        override fun findById(id: ObjectId): BookEntity {
            return INSTANCE.findById(BookEntity::class.java, id) as BookEntity
        }
    }

    @BsonProperty("bookTitle")
    var title: String? = null
        private set
    var author: String? = null
        private set

    @BsonIgnore
    var transientDescription: String? = null

    @JsonbDateFormat("yyyy-MM-dd")
    var creationDate: LocalDate? = null
    var categories = listOf<String>()
        private set
    private var details: BookDetail? = null

    fun setTitle(title: String?): BookEntity {
        this.title = title
        return this
    }

    fun setAuthor(author: String?): BookEntity {
        this.author = author
        return this
    }

    fun setCategories(categories: List<String>): BookEntity {
        this.categories = categories
        return this
    }

    fun getDetails(): BookDetail? {
        return details
    }

    fun setDetails(details: BookDetail?): BookEntity {
        this.details = details
        return this
    }

}