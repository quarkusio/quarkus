package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;

public class BasicServerJacksonMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private final ObjectWriter defaultWriter;

    @Inject
    public BasicServerJacksonMessageBodyWriter(ObjectMapper mapper) {
        mapper.registerModule(MappingModuleHolder.mappingModule);
        this.defaultWriter = createDefaultWriter(mapper);
    }

    static class MappingModuleHolder {
        static final SimpleModule mappingModule = createMappingModule();

        private static SimpleModule createMappingModule() {
            SimpleModule module = new SimpleModule();

            for (Class<? extends StdSerializer> serClass : ResteasyReactiveServerJacksonRecorder.getGeneratedSerializers()) {
                try {
                    StdSerializer serializer = serClass.getConstructor().newInstance();
                    module.addSerializer(serializer.handledType(), serializer);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            for (Class<? extends StdDeserializer> deserClass : ResteasyReactiveServerJacksonRecorder
                    .getGeneratedDeserializers()) {
                try {
                    StdDeserializer deserializer = deserClass.getConstructor().newInstance();
                    module.addDeserializer(deserializer.handledType(), deserializer);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            return module;
        }
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        OutputStream stream = context.getOrCreateOutputStream();
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            defaultWriter.writeValue(stream, o);
        }
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, defaultWriter);
    }

}
