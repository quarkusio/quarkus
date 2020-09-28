package io.quarkus.rest.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import io.quarkus.rest.runtime.util.CaseInsensitiveMap;

public class QuarkusRestClientReaderInterceptorContext extends QuarkusRestAbstractClientInterceptorContext
        implements ReaderInterceptorContext {

    final MessageBodyReader reader;
    InputStream inputStream;
    boolean done = false;
    private int index = 0;
    private final ReaderInterceptor[] interceptors;
    private final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

    public QuarkusRestClientReaderInterceptorContext(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType,
            Map<String, Object> properties, MultivaluedMap<String, String> headers,
            MessageBodyReader reader, InputStream inputStream,
            ReaderInterceptor[] interceptors) {
        super(annotations, entityClass, entityType, mediaType, properties);
        this.reader = reader;
        this.inputStream = inputStream;
        this.interceptors = interceptors;
        this.headers.putAll(headers);
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            return reader.readFrom(entityClass, entityType, annotations, mediaType, headers, inputStream);
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
