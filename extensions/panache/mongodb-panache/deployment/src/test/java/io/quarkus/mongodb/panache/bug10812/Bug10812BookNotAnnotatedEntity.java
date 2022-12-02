package io.quarkus.mongodb.panache.bug10812;

import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

public class Bug10812BookNotAnnotatedEntity extends PanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;

    public String getTitle() {
        return title;
    }

    public Bug10812BookNotAnnotatedEntity setTitle(String title) {
        this.title = title;
        return this;
    }
}
