package io.quarkus.it.mongodb.panache.book;

import java.time.LocalDate;

import javax.json.bind.annotation.JsonbDateFormat;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class BookShortView {
    @BsonProperty("bookTitle")
    private String title; // use the field name title and not the column name bookTitle
    private String author;
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate creationDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
