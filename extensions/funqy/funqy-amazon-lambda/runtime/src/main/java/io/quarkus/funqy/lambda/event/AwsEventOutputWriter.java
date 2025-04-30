package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.LambdaOutputWriter;

/**
 * Responsible for serializing the different data models of the events
 */
public class AwsEventOutputWriter implements LambdaOutputWriter {

    final ObjectMapper mapper;

    public AwsEventOutputWriter(ObjectMapper mapper) {
        // At the moment no special configuration is needed. But we need an ObjectMapper due to different models.
        this.mapper = mapper;
    }

    @Override
    public void writeValue(OutputStream os, Object obj) throws IOException {
        try {
            mapper.writeValue(os, obj);
        } catch (JacksonException e) {
            // to make sure that we do not expose too many details about the issue in the lambda response
            // we have some special treatment for jackson related issues
            throw new IllegalArgumentException("Could not serialize the provided output.", e);
        }
    }

    @Override
    public void writeHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", "application/json");
    }
}
