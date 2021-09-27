package io.quarkus.it.kafka.codecs;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class MovieListDeserializer extends ObjectMapperDeserializer<List<Movie>> {
    public MovieListDeserializer() {
        super(new TypeReference<List<Movie>>() {
        });
    }
}
