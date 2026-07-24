package io.quarkus.it.kafka.codecs;

import java.util.List;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import tools.jackson.core.type.TypeReference;

public class MovieListDeserializer extends ObjectMapperDeserializer<List<Movie>> {
    public MovieListDeserializer() {
        super(new TypeReference<List<Movie>>() {
        });
    }
}
