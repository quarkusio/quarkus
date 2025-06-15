package org.jboss.resteasy.reactive.client.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;

public interface MissingMessageBodyReaderErrorMessageContextualizer {

    /**
     * Takes the same input as
     * {@link jakarta.ws.rs.ext.MessageBodyReader#isReadable(Class, Type, Annotation[], MediaType)} and returns a
     * {@code String} that contextualizes the error message. The result can be null if this class is not able to provide
     * any useful context information.
     */
    String provideContextMessage(Input input);

    interface Input {

        Class<?> type();

        Type genericType();

        Annotation[] annotations();

        MediaType mediaType();
    }
}
