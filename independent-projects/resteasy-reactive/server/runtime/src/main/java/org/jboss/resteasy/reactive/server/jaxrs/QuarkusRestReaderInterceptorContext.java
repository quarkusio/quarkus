package org.jboss.resteasy.reactive.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;

public class QuarkusRestReaderInterceptorContext extends AbstractInterceptorContext
        implements ReaderInterceptorContext {

    private final MessageBodyReader reader;
    private InputStream inputStream;
    private int index = 0;
    private final ReaderInterceptor[] interceptors;
    private final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

    public QuarkusRestReaderInterceptorContext(ResteasyReactiveRequestContext context, Annotation[] annotations, Class<?> type,
            Type genericType, MediaType mediaType, MessageBodyReader reader, InputStream inputStream,
            ReaderInterceptor[] interceptors, ServerSerialisers serialisers) {
        super(context, annotations, type, genericType, mediaType, serialisers);
        this.reader = reader;
        this.inputStream = inputStream;
        this.interceptors = interceptors;
        this.headers.putAll(context.getHttpHeaders().getRequestHeaders());
    }

    @Override
    public Object proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            MessageBodyReader effectiveReader = reader;
            if (rediscoveryNeeded) {
                List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, mediaType, RuntimeType.SERVER);
                if (readers.isEmpty()) {
                    throw new NotSupportedException();
                }
                effectiveReader = readers.get(0);
            }
            return effectiveReader.readFrom(type, genericType, annotations, mediaType, headers, inputStream);
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
