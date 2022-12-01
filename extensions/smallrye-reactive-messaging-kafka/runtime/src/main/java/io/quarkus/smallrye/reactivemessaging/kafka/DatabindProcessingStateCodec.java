package io.quarkus.smallrye.reactivemessaging.kafka;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingStateCodec;
import io.vertx.core.json.jackson.DatabindCodec;

public class DatabindProcessingStateCodec implements ProcessingStateCodec {

    @ApplicationScoped
    public static class Factory implements ProcessingStateCodec.Factory {

        @Override
        public ProcessingStateCodec create(Class<?> stateType) {
            ObjectMapper mapper = DatabindCodec.mapper();
            TypeFactory typeFactory = mapper.getTypeFactory();
            JavaType javaType;
            if (stateType != null) {
                javaType = typeFactory.constructParametricType(ProcessingState.class, stateType);
            } else {
                javaType = typeFactory.constructType(ProcessingState.class);
            }
            ObjectReader reader = mapper.readerFor(javaType);
            ObjectWriter writer = mapper.writerFor(javaType);
            return new DatabindProcessingStateCodec(reader, writer);
        }
    }

    private final ObjectReader reader;
    private final ObjectWriter writer;

    public DatabindProcessingStateCodec(ObjectReader reader, ObjectWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public ProcessingState<?> decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return reader.readValue(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encode(ProcessingState<?> object) {
        try {
            return writer.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
