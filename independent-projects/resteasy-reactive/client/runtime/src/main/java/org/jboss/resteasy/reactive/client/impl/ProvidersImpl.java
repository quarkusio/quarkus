package org.jboss.resteasy.reactive.client.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

public class ProvidersImpl implements Providers {

    private final RestClientRequestContext context;

    public ProvidersImpl(RestClientRequestContext context) {
        this.context = context;
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        List<MessageBodyReader<?>> readers = context.getRestClient().getClientContext().getSerialisers()
                .findReaders(context.getConfiguration(), type, mediaType, RuntimeType.CLIENT);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(type, genericType, annotations, mediaType)) {
                return (MessageBodyReader<T>) reader;
            }
        }
        return null;
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        List<MessageBodyWriter<?>> writers = context.getRestClient().getClientContext().getSerialisers()
                .findWriters(context.getConfiguration(), type, mediaType, RuntimeType.CLIENT);
        for (MessageBodyWriter<?> writer : writers) {
            if (writer.isWriteable(type, genericType, annotations, mediaType)) {
                return (MessageBodyWriter<T>) writer;
            }
        }
        return null;
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        throw new UnsupportedOperationException(
                "`jakarta.ws.rs.ext.ExceptionMapper` are not supported in REST Client Reactive");
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        // TODO: support getting context resolver by mediaType (which is provided using the `@Produces` annotation).
        return context.getConfiguration().getContextResolver(contextType);
    }
}
