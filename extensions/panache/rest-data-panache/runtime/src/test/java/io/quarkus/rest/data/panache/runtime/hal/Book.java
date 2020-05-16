package io.quarkus.rest.data.panache.runtime.hal;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Book {

    public final long id;

    @JsonProperty("book-name")
    @JsonbProperty("book-name")
    private final String name;

    @JsonIgnore
    @JsonbTransient
    public final String ignored = "ignore me";

    public Book(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
