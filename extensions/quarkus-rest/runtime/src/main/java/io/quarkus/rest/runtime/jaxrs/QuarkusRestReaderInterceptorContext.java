package io.quarkus.rest.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.util.CaseInsensitiveMap;

public class QuarkusRestReaderInterceptorContext extends QuarkusRestAbstractInterceptorContext
        implements ReaderInterceptorContext {

    final MessageBodyReader reader;
    InputStream inputStream;
    boolean done = false;
    private int index = 0;
    private final ReaderInterceptor[] interceptors;
    private final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

    public QuarkusRestReaderInterceptorContext(QuarkusRestRequestContext context, Annotation[] annotations, Class<?> type,
            Type genericType, MediaType mediaType, MessageBodyReader reader, InputStream inputStream,
            ReaderInterceptor[] interceptors) {
        super(context, annotations, type, genericType, mediaType);
        this.reader = reader;
        this.inputStream = inputStream;
        this.interceptors = interceptors;
        this.headers.putAll(context.getHttpHeaders().getRequestHeaders());
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            return reader.readFrom(type, genericType, annotations, mediaType, headers, inputStream);
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
