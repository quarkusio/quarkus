package io.quarkus.it.mongodb.panache.book;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "TheBook", clientName = "cl2")
public class Book {
    @BsonProperty("bookTitle")
    private String title;
    private String author;
    private ObjectId id;
    @BsonIgnore
    private String transientDescription;
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate creationDate;

    private List<String> categories = new ArrayList<>();

    private BookDetail details;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Book setAuthor(String author) {
        this.author = author;
        return this;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Book setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public BookDetail getDetails() {
        return details;
    }

    public Book setDetails(BookDetail details) {
        this.details = details;
        return this;
    }

    public String getTransientDescription() {
        return transientDescription;
    }

    public Book setTransientDescription(String transientDescription) {
        this.transientDescription = transientDescription;
        return this;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
