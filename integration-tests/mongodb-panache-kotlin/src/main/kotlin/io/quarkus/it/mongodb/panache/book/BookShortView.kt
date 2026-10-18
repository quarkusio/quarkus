package io.quarkus.it.mongodb.panache.book

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape
import io.quarkus.mongodb.panache.common.ProjectionFor
import java.time.LocalDate

@ProjectionFor(Book::class)
class BookShortView {
    // uses the field name title and not the column name bookTitle
    var title: String? = null
    var author: String? = null

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd") var creationDate: LocalDate? = null
}
