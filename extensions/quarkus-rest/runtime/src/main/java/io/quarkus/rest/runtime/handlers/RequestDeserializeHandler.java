package io.quarkus.rest.runtime.handlers;

import java.io.InputStream;
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

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestReaderInterceptorContext;

public class RequestDeserializeHandler implements ServerRestHandler {

    private final Class<?> type;
    private final MediaType mediaType;
    private final Serialisers serialisers;
    private final int parameterIndex;

    public RequestDeserializeHandler(Class<?> type, MediaType mediaType, Serialisers serialisers, int parameterIndex) {
        this.type = type;
        this.mediaType = mediaType;
        this.serialisers = serialisers;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        MediaType requestType = mediaType;
        String requestTypeString = requestContext.getContext().request().getHeader(HttpHeaders.CONTENT_TYPE);
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
        InputStream in = requestContext.getInputStream();
        Annotation[] annotations = requestContext.getTarget().getLazyMethod().getParameterAnnotations(parameterIndex);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(type, type, annotations, requestType)) {
                Object result;
                ReaderInterceptor[] interceptors = requestContext.getReaderInterceptors();
                try {
                    try {
                        if (interceptors == null) {
                            result = reader.readFrom((Class) type, type, annotations, requestType,
                                    requestContext.getHttpHeaders().getRequestHeaders(), in);
                        } else {
                            result = new QuarkusRestReaderInterceptorContext(requestContext,
                                    annotations,
                                    type, type, requestType, reader, in, interceptors, serialisers).proceed();
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
}
