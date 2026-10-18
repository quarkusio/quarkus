package org.acme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.quarkus.Movie;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

@ApplicationScoped
public class MovieSerde {
    public byte[] serialize(Movie movie) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<Movie> movieWriter = new SpecificDatumWriter<>(Movie.class);
        try (DataFileWriter<Movie> writer = new DataFileWriter<>(movieWriter)) {
            writer.create(Movie.getClassSchema(), out);
            writer.append(movie);
            writer.flush();
        }
        return out.toByteArray();
    }

    public Movie deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DatumReader<Movie> movieReader = new SpecificDatumReader<>(Movie.class);
        try (DataFileStream<Movie> movieStream = new DataFileStream<>(in, movieReader)) {
            return movieStream.next();
        }
    }
}
