package io.quarkus.mongodb.panache.bug10812;

import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

@MongoEntity(collection = "TheBookEntity", clientName = "cl1-10812")
public class Bug10812BookClient1ReactiveEntity extends ReactivePanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;

    public String getTitle() {
        return title;
    }

    public Bug10812BookClient1ReactiveEntity setTitle(String title) {
        this.title = title;
        return this;
    }
}
