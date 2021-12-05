package io.quarkus.it.mongodb.panache.book

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.LocalDate
import javax.json.bind.annotation.JsonbDateFormat

@MongoEntity(collection = "TheBook", clientName = "cl2")
class Book {
    @BsonProperty("bookTitle")
    var title: String? = null
        private set
    var author: String? = null
        private set
    var id: ObjectId? = null

    @BsonIgnore
    var transientDescription: String? = null
        private set

    @JsonbDateFormat("yyyy-MM-dd")
    var creationDate: LocalDate? = null
    var categories = listOf<String>()
        private set
    private var details: BookDetail? = null

    fun setTitle(title: String?): Book {
        this.title = title
        return this
    }

    fun setAuthor(author: String?): Book {
        this.author = author
        return this
    }

    fun setCategories(categories: List<String>): Book {
        this.categories = categories
        return this
    }

    fun getDetails(): BookDetail? {
        return details
    }

    fun setDetails(details: BookDetail?): Book {
        this.details = details
        return this
    }

    fun setTransientDescription(transientDescription: String?): Book {
        this.transientDescription = transientDescription
        return this
    }

}