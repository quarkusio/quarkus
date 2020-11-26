package org.jboss.resteasy.reactive.server.handlers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestReaderInterceptorContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class RequestDeserializeHandler implements ServerRestHandler {

    private final Class<?> type;
    private final MediaType mediaType;
    private final ServerSerialisers serialisers;
    private final int parameterIndex;

    public RequestDeserializeHandler(Class<?> type, MediaType mediaType, ServerSerialisers serialisers, int parameterIndex) {
        this.type = type;
        this.mediaType = mediaType;
        this.serialisers = serialisers;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        MediaType requestType = mediaType;
        String requestTypeString = requestContext.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
        if (requestTypeString != null) {
            try {
                requestType = MediaType.valueOf(requestTypeString);
            } catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
        } else if (requestType == null) {
            requestType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, requestType, RuntimeType.SERVER);
        if (readers.isEmpty()) {
            throw new NotSupportedException();
        }
        for (MessageBodyReader<?> reader : readers) {
            if (isReadable(reader, requestContext, requestType)) {
                Object result;
                ReaderInterceptor[] interceptors = requestContext.getReaderInterceptors();
                try {
                    try {
                        if (interceptors == null) {
                            result = readFrom(reader, requestContext, requestType);
                        } else {
                            result = new QuarkusRestReaderInterceptorContext(requestContext,
                                    getAnnotations(requestContext),
                                    type, type, requestType, reader, requestContext.getInputStream(), interceptors, serialisers)
                                            .proceed();
                        }
                    } catch (NoContentException e) {
                        throw new BadRequestException(e);
                    }
                } catch (Exception e) {
                    requestContext.resume(e);
                    return;
                }
                requestContext.setRequestEntity(result);
                requestContext.resume();
                return;
            }
        }
        throw new NotSupportedException();
    }

    private boolean isReadable(MessageBodyReader<?> reader, ResteasyReactiveRequestContext requestContext,
            MediaType requestType) {
        if (reader instanceof QuarkusRestMessageBodyReader) {
            return ((QuarkusRestMessageBodyReader<?>) reader).isReadable(type, type, requestContext.getTarget().getLazyMethod(),
                    requestType);
        }
        return reader.isReadable(type, type, getAnnotations(requestContext), requestType);
    }

    @SuppressWarnings("unchecked")
    public Object readFrom(MessageBodyReader<?> reader, ResteasyReactiveRequestContext requestContext, MediaType requestType)
            throws IOException {
        if (reader instanceof QuarkusRestMessageBodyReader) {
            return ((QuarkusRestMessageBodyReader<?>) reader).readFrom((Class) type, type, requestType, requestContext);
        }
        return reader.readFrom((Class) type, type, getAnnotations(requestContext), requestType,
                requestContext.getHttpHeaders().getRequestHeaders(), requestContext.getInputStream());
    }

    private Annotation[] getAnnotations(ResteasyReactiveRequestContext requestContext) {
        return requestContext.getTarget().getLazyMethod().getParameterAnnotations(parameterIndex);
    }
}
