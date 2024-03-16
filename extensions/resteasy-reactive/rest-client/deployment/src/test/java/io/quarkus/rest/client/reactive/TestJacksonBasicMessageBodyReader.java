package io.quarkus.rest.client.reactive;

import jakarta.ws.rs.Consumes;

import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;

import com.fasterxml.jackson.databind.ObjectMapper;

@Consumes({ "application/json", "application/x-ndjson", "application/stream+json" })
public class TestJacksonBasicMessageBodyReader extends JacksonBasicMessageBodyReader {
    public TestJacksonBasicMessageBodyReader() {
        super(new ObjectMapper());
    }
}
