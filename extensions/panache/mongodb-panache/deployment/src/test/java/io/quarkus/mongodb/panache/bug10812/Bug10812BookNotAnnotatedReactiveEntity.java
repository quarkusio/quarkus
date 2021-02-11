package io.quarkus.mongodb.panache.bug10812;

import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class Bug10812BookNotAnnotatedReactiveEntity extends ReactivePanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;

    public String getTitle() {
        return title;
    }

    public Bug10812BookNotAnnotatedReactiveEntity setTitle(String title) {
        this.title = title;
        return this;
    }
}
