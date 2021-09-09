package org.jboss.resteasy.reactive.server.handlers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.jaxrs.ReaderInterceptorContextImpl;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class RequestDeserializeHandler implements ServerRestHandler {

    private static final Logger log = Logger.getLogger(RequestDeserializeHandler.class);

    private final Class<?> type;
    private final Type genericType;
    private final MediaType mediaType;
    private final ServerSerialisers serialisers;
    private final int parameterIndex;

    public RequestDeserializeHandler(Class<?> type, Type genericType, MediaType mediaType, ServerSerialisers serialisers,
            int parameterIndex) {
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.serialisers = serialisers;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        MediaType effectiveRequestType = mediaType;
        String requestTypeString = requestContext.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
        if (requestTypeString != null) {
            try {
                effectiveRequestType = MediaTypeHelper.withSuffixAsSubtype(MediaType.valueOf(requestTypeString));
            } catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
        } else if (effectiveRequestType == null) {
            effectiveRequestType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, effectiveRequestType, RuntimeType.SERVER);
        if (readers.isEmpty()) {
            throw new NotSupportedException();
        }
        for (MessageBodyReader<?> reader : readers) {
            if (isReadable(reader, requestContext, effectiveRequestType)) {
                Object result;
                ReaderInterceptor[] interceptors = requestContext.getReaderInterceptors();
                try {
                    try {
                        if (interceptors == null) {
                            result = readFrom(reader, requestContext, effectiveRequestType);
                        } else {
                            result = new ReaderInterceptorContextImpl(requestContext,
                                    getAnnotations(requestContext),
                                    type, genericType, effectiveRequestType, reader, requestContext.getInputStream(),
                                    interceptors,
                                    serialisers)
                                            .proceed();
                        }
                    } catch (NoContentException e) {
                        throw new BadRequestException(e);
                    }
                } catch (Exception e) {
                    log.debug("Error occurred during deserialization of input", e);
                    requestContext.handleException(e, true);
                    requestContext.resume();
                    return;
                }
                requestContext.setRequestEntity(result);
                requestContext.resume();
                return;
            }
        }
        throw new NotSupportedException("No supported MessageBodyReader found");
    }

    private boolean isReadable(MessageBodyReader<?> reader, ResteasyReactiveRequestContext requestContext,
            MediaType requestType) {
        if (reader instanceof ServerMessageBodyReader) {
            return ((ServerMessageBodyReader<?>) reader).isReadable(type, genericType,
                    requestContext.getTarget().getLazyMethod(),
                    requestType);
        }
        return reader.isReadable(type, genericType, getAnnotations(requestContext), requestType);
    }

    @SuppressWarnings("unchecked")
    public Object readFrom(MessageBodyReader<?> reader, ResteasyReactiveRequestContext requestContext, MediaType requestType)
            throws IOException {
        if (reader instanceof ServerMessageBodyReader) {
            return ((ServerMessageBodyReader<?>) reader).readFrom((Class) type, genericType, requestType, requestContext);
        }
        return reader.readFrom((Class) type, genericType, getAnnotations(requestContext), requestType,
                requestContext.getHttpHeaders().getRequestHeaders(), requestContext.getInputStream());
    }

    private Annotation[] getAnnotations(ResteasyReactiveRequestContext requestContext) {
        return requestContext.getTarget().getLazyMethod().getParameterAnnotations(parameterIndex);
    }
}
