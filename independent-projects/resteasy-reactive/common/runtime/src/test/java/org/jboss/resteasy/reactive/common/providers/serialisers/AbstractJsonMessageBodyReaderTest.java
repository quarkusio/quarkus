package org.jboss.resteasy.reactive.common.providers.serialisers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

class AbstractJsonMessageBodyReaderTest {

    class TestReader extends AbstractJsonMessageBodyReader {
        @Override
        public Object readFrom(Class<Object> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
                throws IOException, WebApplicationException {
            return null;
        }
    }

    @Test
    void isReadableCaseInsensitive() {
        final TestReader testReader = new TestReader();
        assertFalse(testReader.isReadable(new MediaType("application", "jso"), Object.class));
        assertFalse(testReader.isReadable(new MediaType("application", "json+anything"), Object.class));
        assertFalse(testReader.isReadable(new MediaType("test", "json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("test", "test+json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("test", "x-ndjson"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("application", "test+json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("application", "json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("Application", "Json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("appliCAtion", "json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("application", "jSOn"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("application", "test+json"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("application", "x-ndjson"), Object.class));
        assertTrue(testReader.isReadable(new MediaType("applIcation", "x-ndjson"), Object.class));
    }
}
