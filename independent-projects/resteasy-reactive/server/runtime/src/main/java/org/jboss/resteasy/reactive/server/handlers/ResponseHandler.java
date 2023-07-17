package org.jboss.resteasy.reactive.server.handlers;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

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
@SuppressWarnings("ForLoopReplaceableByForEach")
public class ResponseHandler implements ServerRestHandler {

    public static final ResponseHandler NO_CUSTOMIZER_INSTANCE = new ResponseHandler();

    private final List<ResponseBuilderCustomizer> responseBuilderCustomizers;

    public ResponseHandler(List<ResponseBuilderCustomizer> responseBuilderCustomizers) {
        this.responseBuilderCustomizers = responseBuilderCustomizers;
    }

    private ResponseHandler() {
        this.responseBuilderCustomizers = Collections.emptyList();
    }

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
            if (!mediaTypeAlreadyExists && (produces != null) && (responseBuilder.getEntity() != null)) {
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
            if (!mediaTypeAlreadyExists && (produces != null) && (responseBuilder.getEntity() != null)) {
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
                        ResponseBuilderImpl responseBuilder;
                        if (result instanceof GenericEntity) {
                            GenericEntity<?> genericEntity = (GenericEntity<?>) result;
                            requestContext.setGenericReturnType(genericEntity.getType());
                            responseBuilder = (ResponseBuilderImpl) ResponseImpl.ok(genericEntity.getEntity());
                        } else if (result == null) {
                            // FIXME: custom status codes depending on method?
                            responseBuilder = (ResponseBuilderImpl) ResponseImpl.noContent();
                        } else {
                            // FIXME: custom status codes depending on method?
                            responseBuilder = (ResponseBuilderImpl) ResponseImpl.ok(result);
                        }
                        if (responseBuilder.getEntity() != null) {
                            EncodedMediaType produces = requestContext.getResponseContentType();
                            if (produces != null) {
                                responseBuilder.header(HttpHeaders.CONTENT_TYPE, produces.toString());
                            }
                        }
                        if (!responseBuilderCustomizers.isEmpty()) {
                            for (int i = 0; i < responseBuilderCustomizers.size(); i++) {
                                responseBuilderCustomizers.get(i).customize(responseBuilder);
                            }
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

                @Override
                public boolean isPredetermined() {
                    return responseBuilderCustomizers.isEmpty();
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

    public interface ResponseBuilderCustomizer {

        void customize(Response.ResponseBuilder responseBuilder);

        class StatusCustomizer implements ResponseBuilderCustomizer {

            private int status;

            public StatusCustomizer(int status) {
                this.status = status;
            }

            public StatusCustomizer() {
            }

            public int getStatus() {
                return status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            @Override
            public void customize(Response.ResponseBuilder responseBuilder) {
                responseBuilder.status(status);
            }
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        class AddHeadersCustomizer implements ResponseBuilderCustomizer {

            private Map<String, List<String>> headers;

            public AddHeadersCustomizer(Map<String, List<String>> headers) {
                this.headers = headers;
            }

            public AddHeadersCustomizer() {
            }

            public Map<String, List<String>> getHeaders() {
                return headers;
            }

            public void setHeaders(Map<String, List<String>> headers) {
                this.headers = headers;
            }

            @Override
            public void customize(Response.ResponseBuilder responseBuilder) {
                for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                    List<String> values = header.getValue();
                    String headerName = header.getKey();
                    if (values.size() == 1) {
                        responseBuilder.header(headerName, values.get(0));
                    } else {
                        for (int i = 0; i < values.size(); i++) {
                            responseBuilder.header(headerName, values.get(i));
                        }
                    }
                }
            }
        }
    }

}
