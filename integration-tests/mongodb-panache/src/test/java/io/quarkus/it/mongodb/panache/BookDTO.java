package io.quarkus.it.mongodb.panache;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.it.mongodb.panache.book.BookDetail;

/**
 * The IT uses a DTO and not directly the Book object because it should avoid the usage of ObjectId.
 */
public class BookDTO {
    private String title;
    private String author;
    private String id;
    private String transientDescription;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date creationDate;

    private List<String> categories = new ArrayList<>();

    private BookDetail details;

    // will only be used in case of projection
    private Integer rating;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public BookDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public BookDTO setAuthor(String author) {
        this.author = author;
        return this;
    }

    public List<String> getCategories() {
        return categories;
    }

    public BookDTO setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public BookDetail getDetails() {
        return details;
    }

    public BookDTO setDetails(BookDetail details) {
        this.details = details;
        return this;
    }

    public String getTransientDescription() {
        return transientDescription;
    }

    public BookDTO setTransientDescription(String transientDescription) {
        this.transientDescription = transientDescription;
        return this;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public BookDTO setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    public String toString() {
        return "BookDTO{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", id='" + id + '\'' +
                ", transientDescription='" + transientDescription + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", categories=" + categories +
                ", details=" + details +
                '}';
    }
}
