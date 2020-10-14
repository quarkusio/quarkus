package io.quarkus.it.mongodb.rest.data.panache;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity
public class Author extends PanacheMongoEntity {

    public String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate dob;
}
