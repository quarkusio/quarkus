package org.acme;

import jakarta.inject.Inject;
import org.acme.quarkus.Movie;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class MovieSerdeTest {
    @Inject
    MovieSerde serializer;

    @Test
    void test() throws Exception {
        Movie movie = new Movie("my-movie");

        byte[] serializedMovie = serializer.serialize(movie);
        Movie deserialized = serializer.deserialize(serializedMovie);

        assertEquals(deserialized, movie);
    }
}
