package io.quarkus.mongodb.panache;

import org.bson.codecs.pojo.annotations.BsonId;

public class DuplicateIdWithParentEntity extends DuplicateIdParentEntity {
    @BsonId
    public String customId;
}
