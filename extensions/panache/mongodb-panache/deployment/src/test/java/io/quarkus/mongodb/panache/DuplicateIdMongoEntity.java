package io.quarkus.mongodb.panache;

import org.bson.codecs.pojo.annotations.BsonId;

public class DuplicateIdMongoEntity extends PanacheMongoEntity {
    @BsonId
    public String customId;
}
