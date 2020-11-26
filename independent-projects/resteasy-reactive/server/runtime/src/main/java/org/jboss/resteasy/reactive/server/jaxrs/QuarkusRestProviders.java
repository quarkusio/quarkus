package org.jboss.resteasy.reactive.server.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import org.jboss.resteasy.reactive.server.core.Deployment;

//TODO: test
public class QuarkusRestProviders implements Providers {

    private final Deployment deployment;

    public QuarkusRestProviders(Deployment deployment) {
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

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        return deployment.getExceptionMapping().getExceptionMapper(type, null);
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return deployment.getContextResolvers().getContextResolver(contextType, mediaType);
    }
}
