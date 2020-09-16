package io.quarkus.mongodb.panache.bug10812;

import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection = "TheBookEntity")
public class Bug10812BookDefaultClientNameEntity extends PanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;

    public String getTitle() {
        return title;
    }

    public Bug10812BookDefaultClientNameEntity setTitle(String title) {
        this.title = title;
        return this;
    }
}
