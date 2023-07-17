package io.quarkus.mongodb.panache.bug10812;

import org.bson.codecs.pojo.annotations.BsonProperty;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "TheBookEntity", clientName = "cl2-10812")
public class Bug10812BookClient2Entity extends PanacheMongoEntity {
    @BsonProperty("bookTitle")
    private String title;

    public String getTitle() {
        return title;
    }

    public Bug10812BookClient2Entity setTitle(String title) {
        this.title = title;
        return this;
    }
}
