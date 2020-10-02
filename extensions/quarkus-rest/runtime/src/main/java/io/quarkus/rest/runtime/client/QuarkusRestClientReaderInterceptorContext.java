package io.quarkus.rest.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.util.CaseInsensitiveMap;

public class QuarkusRestClientReaderInterceptorContext extends QuarkusRestAbstractClientInterceptorContext
        implements ReaderInterceptorContext {

    final QuarkusRestConfiguration configuration;
    final Serialisers serialisers;
    InputStream inputStream;
    boolean done = false;
    private int index = 0;
    private final ReaderInterceptor[] interceptors;
    private final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
    private final RuntimeType runtimeType;

    public QuarkusRestClientReaderInterceptorContext(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType,
            Map<String, Object> properties, MultivaluedMap<String, String> headers,
            QuarkusRestConfiguration configuration, Serialisers serialisers, InputStream inputStream,
            ReaderInterceptor[] interceptors, RuntimeType runtimeType) {
        super(annotations, entityClass, entityType, mediaType, properties);
        this.configuration = configuration;
        this.serialisers = serialisers;
        this.inputStream = inputStream;
        this.interceptors = interceptors;
        this.runtimeType = runtimeType;
        this.headers.putAll(headers);
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            List<MessageBodyReader<?>> readers = serialisers.findReaders(configuration, entityClass, mediaType,
                    runtimeType);
            for (MessageBodyReader<?> reader : readers) {
                if (reader.isReadable(entityClass, entityType, annotations, mediaType)) {
                    try {
                        return ((MessageBodyReader) reader).readFrom(entityClass, entityType, annotations, mediaType, headers,
                                inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
            // Spec says to throw this
            throw new ProcessingException(
                    "Request could not be mapped to type " + entityType);
        } else {
            return interceptors[index++].aroundReadFrom(this);
        }
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

}
