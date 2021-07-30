package org.jboss.resteasy.reactive.server.handlers;

import java.io.ByteArrayInputStream;
import java.util.List;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.jaxrs.RestResponseImpl;
import org.jboss.resteasy.reactive.server.core.EncodedMediaType;
import org.jboss.resteasy.reactive.server.core.LazyResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ResponseBuilderImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Our job is to turn endpoint return types into Response instances
 */
public class ResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Object result = requestContext.getResult();
        if (result instanceof Response) {
            boolean mediaTypeAlreadyExists = false;
            //we already have a response
            //set it explicitly
            ResponseBuilderImpl responseBuilder;
            Response existing = (Response) result;
            if (existing.getEntity() instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) existing.getEntity();
                requestContext.setGenericReturnType(genericEntity.getType());
                responseBuilder = fromResponse(existing);
                responseBuilder.entity(genericEntity.getEntity());
            } else {
                // TCK says to use the entity type as generic type if we return a response
                if (existing.hasEntity() && (existing.getEntity() != null))
                    requestContext.setGenericReturnType(existing.getEntity().getClass());
                //TODO: super inefficient
                responseBuilder = fromResponse(existing);
                if ((result instanceof ResponseImpl)) {
                    // needed in order to preserve entity annotations
                    ResponseImpl responseImpl = (ResponseImpl) result;
                    if (responseImpl.getEntityAnnotations() != null) {
                        requestContext.setAdditionalAnnotations(responseImpl.getEntityAnnotations());
                    }

                    // this is a weird case where the response comes from the the rest-client
                    if (responseBuilder.getEntity() == null) {
                        if (responseImpl.getEntityStream() instanceof ByteArrayInputStream) {
                            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) responseImpl.getEntityStream();
                            responseBuilder.entity(byteArrayInputStream.readAllBytes());
                        }
                    }
                }
            }
            if (existing.getMediaType() != null) {
                requestContext.setResponseContentType(existing.getMediaType());
                mediaTypeAlreadyExists = true;
            }
            EncodedMediaType produces = requestContext.getResponseContentType();
            if (!mediaTypeAlreadyExists && produces != null) {
                responseBuilder.header(HttpHeaders.CONTENT_TYPE, produces.toString());
            }
            if ((responseBuilder instanceof ResponseBuilderImpl)) {
                // avoid unnecessary copying of HTTP headers from the Builder to the Response
                requestContext
                        .setResponse(
                                new LazyResponse.Existing(((ResponseBuilderImpl) responseBuilder).build(false)));
            } else {
                requestContext.setResponse(new LazyResponse.Existing(responseBuilder.build()));
            }
        } else if (result instanceof RestResponse) {
            boolean mediaTypeAlreadyExists = false;
            //we already have a response
            //set it explicitly
            ResponseBuilderImpl responseBuilder;
            RestResponse<?> existing = (RestResponse<?>) result;
            if (existing.getEntity() instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) existing.getEntity();
                requestContext.setGenericReturnType(genericEntity.getType());
                responseBuilder = fromResponse(existing);
                responseBuilder.entity(genericEntity.getEntity());
            } else {
                // TCK says to use the entity type as generic type if we return a response
                if (existing.hasEntity() && (existing.getEntity() != null))
                    requestContext.setGenericReturnType(existing.getEntity().getClass());
                //TODO: super inefficient
                responseBuilder = fromResponse(existing);
                if ((result instanceof RestResponseImpl)) {
                    // needed in order to preserve entity annotations
                    RestResponseImpl<?> responseImpl = (RestResponseImpl<?>) result;
                    if (responseImpl.getEntityAnnotations() != null) {
                        requestContext.setAdditionalAnnotations(responseImpl.getEntityAnnotations());
                    }

                    // this is a weird case where the response comes from the the rest-client
                    if (responseBuilder.getEntity() == null) {
                        if (responseImpl.getEntityStream() instanceof ByteArrayInputStream) {
                            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) responseImpl.getEntityStream();
                            responseBuilder.entity(byteArrayInputStream.readAllBytes());
                        }
                    }
                }
            }
            if (existing.getMediaType() != null) {
                requestContext.setResponseContentType(existing.getMediaType());
                mediaTypeAlreadyExists = true;
            }
            EncodedMediaType produces = requestContext.getResponseContentType();
            if (!mediaTypeAlreadyExists && produces != null) {
                responseBuilder.header(HttpHeaders.CONTENT_TYPE, produces.toString());
            }
            if ((responseBuilder instanceof ResponseBuilderImpl)) {
                // avoid unnecessary copying of HTTP headers from the Builder to the Response
                requestContext
                        .setResponse(
                                new LazyResponse.Existing(((ResponseBuilderImpl) responseBuilder).build(false)));
            } else {
                requestContext.setResponse(new LazyResponse.Existing(responseBuilder.build()));
            }
        } else {
            requestContext.setResponse(new LazyResponse() {

                Response response;

                @Override
                public Response get() {
                    if (response == null) {
                        Response.ResponseBuilder responseBuilder;
                        if (result instanceof GenericEntity) {
                            GenericEntity<?> genericEntity = (GenericEntity<?>) result;
                            requestContext.setGenericReturnType(genericEntity.getType());
                            responseBuilder = ResponseImpl.ok(genericEntity.getEntity());
                        } else if (result == null) {
                            // FIXME: custom status codes depending on method?
                            responseBuilder = ResponseImpl.noContent();
                        } else {
                            // FIXME: custom status codes depending on method?
                            responseBuilder = ResponseImpl.ok(result);
                        }
                        EncodedMediaType produces = requestContext.getResponseContentType();
                        if (produces != null) {
                            responseBuilder.header(HttpHeaders.CONTENT_TYPE, produces.toString());
                        }
                        if ((responseBuilder instanceof ResponseBuilderImpl)) {
                            // avoid unnecessary copying of HTTP headers from the Builder to the Response
                            response = ((ResponseBuilderImpl) responseBuilder).build(false);
                        } else {
                            response = responseBuilder.build();
                        }
                    }
                    return response;
                }

                @Override
                public boolean isCreated() {
                    return response != null;
                }
            });

        }
    }

    // avoid the runtime overhead of looking up the provider
    private ResponseBuilderImpl fromResponse(Response response) {
        Response.ResponseBuilder b = new ResponseBuilderImpl().status(response.getStatus());
        if (response.hasEntity()) {
            b.entity(response.getEntity());
        }
        for (String headerName : response.getHeaders().keySet()) {
            List<Object> headerValues = response.getHeaders().get(headerName);
            for (Object headerValue : headerValues) {
                b.header(headerName, headerValue);
            }
        }
        return (ResponseBuilderImpl) b;
    }

    // avoid the runtime overhead of looking up the provider
    private ResponseBuilderImpl fromResponse(RestResponse<?> response) {
        Response.ResponseBuilder b = new ResponseBuilderImpl().status(response.getStatus());
        if (response.hasEntity()) {
            b.entity(response.getEntity());
        }
        for (String headerName : response.getHeaders().keySet()) {
            List<Object> headerValues = response.getHeaders().get(headerName);
            for (Object headerValue : headerValues) {
                b.header(headerName, headerValue);
            }
        }
        return (ResponseBuilderImpl) b;
    }
}
