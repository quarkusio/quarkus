package org.jboss.resteasy.reactive.server.jaxrs;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.server.core.Deployment;

public class ProvidersImpl implements Providers {

    private final Deployment deployment;

    public ProvidersImpl(Deployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        List<MessageBodyReader<?>> readers = deployment.getSerialisers().findReaders(null, type, mediaType);
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
        List<MessageBodyWriter<?>> writers = deployment.getSerialisers().findWriters(null, type, mediaType, RuntimeType.SERVER);
        for (MessageBodyWriter<?> writer : writers) {
            if (writer.isWriteable(type, genericType, annotations, mediaType)) {
                return (MessageBodyWriter<T>) writer;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        Map.Entry<Throwable, ExceptionMapper<? extends Throwable>> entry = deployment.getExceptionMapper()
                .getExceptionMapper(type, null, null);
        if (entry != null) {
            return (ExceptionMapper<T>) entry.getValue();
        }
        return null;
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return deployment.getContextResolvers().getContextResolver(contextType, mediaType);
    }
}
