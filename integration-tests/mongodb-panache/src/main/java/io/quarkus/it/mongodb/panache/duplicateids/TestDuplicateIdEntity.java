package io.quarkus.it.mongodb.panache.duplicateids;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

public class TestDuplicateIdEntity extends PanacheMongoEntityBase {

    @BsonProperty(value = "_id")
    public ObjectId objectId;

    public String id;

    public TestDuplicateIdEntity() {
    }

    public TestDuplicateIdEntity(String id) {
        this.id = id;
    }
}
