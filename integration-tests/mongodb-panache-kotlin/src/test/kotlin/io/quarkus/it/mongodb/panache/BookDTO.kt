package io.quarkus.it.mongodb.panache

import com.fasterxml.jackson.annotation.JsonFormat
import io.quarkus.it.mongodb.panache.book.BookDetail
import java.util.ArrayList
import java.util.Date

/**
 * The IT uses a DTO and not directly the Book object because it should avoid the usage of ObjectId.
 */
class BookDTO {
    var title: String? = null
        private set

    var author: String? = null
        private set

    var id: String? = null
    var transientDescription: String? = null
        private set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private var creationDate: Date? = null
    var categories: List<String> = ArrayList()
        private set

    var details: BookDetail? = null
        private set

    fun setTitle(title: String?): BookDTO {
        this.title = title
        return this
    }

    fun setAuthor(author: String?): BookDTO {
        this.author = author
        return this
    }

    fun setCategories(categories: List<String>): BookDTO {
        this.categories = categories
        return this
    }

    fun setDetails(details: BookDetail?): BookDTO {
        this.details = details
        return this
    }

    fun setTransientDescription(transientDescription: String?): BookDTO {
        this.transientDescription = transientDescription
        return this
    }

    fun getCreationDate(): Date? {
        return creationDate
    }

    fun setCreationDate(creationDate: Date?): BookDTO {
        this.creationDate = creationDate
        return this
    }

    @Override
    override fun toString(): String {
        return "BookDTO{" +
            "title='" +
            title +
            '\'' +
            ", author='" +
            author +
            '\'' +
            ", id='" +
            id +
            '\'' +
            ", transientDescription='" +
            transientDescription +
            '\'' +
            ", creationDate='" +
            creationDate +
            '\'' +
            ", categories=" +
            categories +
            ", details=" +
            details +
            '}'
    }
}
