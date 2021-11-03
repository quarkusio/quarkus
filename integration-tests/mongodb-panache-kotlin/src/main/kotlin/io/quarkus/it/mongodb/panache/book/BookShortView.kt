package io.quarkus.it.mongodb.panache.book

import io.quarkus.mongodb.panache.common.ProjectionFor
import java.time.LocalDate
import javax.json.bind.annotation.JsonbDateFormat

@ProjectionFor(Book::class)
class BookShortView {
    // uses the field name title and not the column name bookTitle
    var title: String? = null
    var author: String? = null

    @JsonbDateFormat("yyyy-MM-dd")
    var creationDate: LocalDate? = null
}