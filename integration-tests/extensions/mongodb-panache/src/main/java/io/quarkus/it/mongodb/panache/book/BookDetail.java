package io.quarkus.it.mongodb.panache.book;

public class BookDetail {

    private String summary;

    private int rating;

    public String getSummary() {
        return summary;
    }

    public BookDetail setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public BookDetail setRating(int rating) {
        this.rating = rating;
        return this;
    }
}
