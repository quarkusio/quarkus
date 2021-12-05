package io.quarkus.it.mongodb.panache.book;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "TheBookEntity", clientName = "cl2")
public class BookEntity extends PanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;
    private String author;
    @BsonIgnore
    private String transientDescription;
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate creationDate;

    private List<String> categories = new ArrayList<>();

    private BookDetail details;

    public String getTitle() {
        return title;
    }

    public BookEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public BookEntity setAuthor(String author) {
        this.author = author;
        return this;
    }

    public List<String> getCategories() {
        return categories;
    }

    public BookEntity setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public BookDetail getDetails() {
        return details;
    }

    public BookEntity setDetails(BookDetail details) {
        this.details = details;
        return this;
    }

    public String getTransientDescription() {
        return transientDescription;
    }

    public void setTransientDescription(String transientDescription) {
        this.transientDescription = transientDescription;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
