package io.quarkus.it.mongodb.panache.book

class BookDetail {
    var summary: String? = null
        private set

    var rating = 0
        private set

    fun setSummary(summary: String?): BookDetail {
        this.summary = summary
        return this
    }

    fun setRating(rating: Int): BookDetail {
        this.rating = rating
        return this
    }
}
