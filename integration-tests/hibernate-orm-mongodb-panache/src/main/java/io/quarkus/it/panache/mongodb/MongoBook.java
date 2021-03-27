package io.quarkus.it.panache.mongodb;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.MongoEntity;

@MongoEntity(collection = "TheBook", clientName = "cl2")
public class MongoBook {
    @BsonProperty("bookTitle")
    private String title;
    private String author;
    private ObjectId id;

    public MongoBook() {
    }

    public MongoBook(final String title, final String author) {
        this.title = title;
        this.author = author;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public MongoBook setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public MongoBook setAuthor(String author) {
        this.author = author;
        return this;
    }
}
