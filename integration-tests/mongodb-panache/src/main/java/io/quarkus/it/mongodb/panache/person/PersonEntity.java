package io.quarkus.it.mongodb.panache.person;

import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.panache.common.Sort;

public class PersonEntity extends PanacheMongoEntityBase {
    @BsonId
    public Long id;
    public String firstname;
    public String lastname;

    public static List<PersonEntity> findOrdered() {
        return findAll(Sort.by("lastname", "firstname")).list();
    }
}
