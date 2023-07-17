package org.jboss.resteasy.reactive.server.handlers;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.ReaderInterceptor;

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
    private final List<MediaType> acceptableMediaTypes;
    private final ServerSerialisers serialisers;
    private final int parameterIndex;

    public RequestDeserializeHandler(Class<?> type, Type genericType, List<MediaType> acceptableMediaTypes,
            ServerSerialisers serialisers,
            int parameterIndex) {
        this.type = type;
        this.genericType = genericType;
        this.acceptableMediaTypes = acceptableMediaTypes;
        this.serialisers = serialisers;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.requireCDIRequestScope();
        MediaType effectiveRequestType = null;
        Object requestType = requestContext.getHeader(HttpHeaders.CONTENT_TYPE, true);
        if (requestType != null) {
            try {
                effectiveRequestType = MediaType.valueOf((String) requestType);
            } catch (Exception e) {
                log.debugv("Incorrect media type", e);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }

            // We need to verify media type for sub-resources, this mimics what is done in {@code ClassRoutingHandler}
            if (MediaTypeHelper.getFirstMatch(
                    acceptableMediaTypes,
                    Collections.singletonList(effectiveRequestType)) == null) {
                throw new NotSupportedException("The content-type header value did not match the value in @Consumes");
            }
        } else if (!acceptableMediaTypes.isEmpty()) {
            effectiveRequestType = acceptableMediaTypes.get(0);
        } else {
            effectiveRequestType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, effectiveRequestType, RuntimeType.SERVER);
        if (readers.isEmpty()) {
            log.debugv("No matching MessageBodyReader found for type {0} and media type {1}", type, effectiveRequestType);
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
        log.debugv("No matching MessageBodyReader found for type {0} and media type {1}", type, effectiveRequestType);
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
        requestContext.requireCDIRequestScope();
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
