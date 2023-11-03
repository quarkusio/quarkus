package io.quarkus.it.mongodb.panache.reactive.transaction;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;

@MongoEntity(database = "transaction-person")
public class ReactiveTransactionPerson extends ReactivePanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;
}
