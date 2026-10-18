package io.quarkus.it.mongodb.panache.book

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity
import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations.Companion.INSTANCE
import java.time.LocalDate
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId

@MongoEntity(collection = "TheBookEntity", clientName = "cl2", readPreference = "primary")
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

    @BsonIgnore var transientDescription: String? = null

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd") var creationDate: LocalDate? = null
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
