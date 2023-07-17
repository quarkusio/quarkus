package io.quarkus.it.mongodb.panache.transaction;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(database = "transaction-person")
public class TransactionPerson extends PanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
}
