package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class ClientJacksonMessageBodyReader extends JacksonBasicMessageBodyReader implements ClientRestHandler {

    private final ConcurrentMap<ObjectMapper, ObjectReader> contextResolverMap = new ConcurrentHashMap<>();
    private RestClientRequestContext context;

    @Inject
    public ClientJacksonMessageBodyReader(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public void handle(RestClientRequestContext requestContext) {
        this.context = requestContext;
    }

    @Override
    protected ObjectReader getEffectiveReader() {
        if (context == null) {
            // no context injected when reader is not running within a rest client context
            return super.getEffectiveReader();
        }

        ObjectMapper objectMapper = context.getConfiguration().getFromContext(ObjectMapper.class);
        if (objectMapper == null) {
            return super.getEffectiveReader();
        }

        return contextResolverMap.computeIfAbsent(objectMapper, new Function<>() {
            @Override
            public ObjectReader apply(ObjectMapper objectMapper) {
                return objectMapper.reader();
            }
        });
    }
}
