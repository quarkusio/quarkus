package org.jboss.resteasy.reactive.client.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;
import org.jboss.resteasy.reactive.client.QuarkusRestAsyncInvoker;
import org.jboss.resteasy.reactive.client.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;

public class ClientSendRequestHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (requestContext.getAbortedWith() != null) {
            return;
        }
        requestContext.suspend();
        HttpClientRequest httpClientRequest = createRequest(requestContext);
        Buffer actualEntity = setRequestHeadersAndPrepareBody(httpClientRequest, requestContext);
        httpClientRequest.handler(new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse clientResponse) {
                try {
                    requestContext.initialiseResponse(clientResponse);
                    if (!requestContext.isRegisterBodyHandler()) {
                        clientResponse.pause();
                        requestContext.resume();
                    } else {
                        clientResponse.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                try {
                                    if (buffer.length() > 0) {
                                        requestContext.setResponseEntityStream(new ByteArrayInputStream(buffer.getBytes()));
                                    } else {
                                        requestContext.setResponseEntityStream(null);
                                    }
                                    requestContext.resume();
                                } catch (Throwable t) {
                                    requestContext.resume(t);
                                }
                            }
                        });
                    }
                } catch (Throwable t) {
                    requestContext.resume(t);
                }
            }
        });
        httpClientRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                if (event instanceof IOException) {
                    requestContext.resume(new ProcessingException(event));
                } else {
                    requestContext.resume(event);
                }
            }
        });
        if (actualEntity == QuarkusRestAsyncInvoker.EMPTY_BUFFER) {
            httpClientRequest.end();
        } else {
            httpClientRequest.end(actualEntity);
        }

    }

    public <T> HttpClientRequest createRequest(RestClientRequestContext state) {
        HttpClient httpClient = state.getHttpClient();
        URI uri = state.getUri();
        HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.valueOf(state.getHttpMethod()), uri.getPort(),
                uri.getHost(),
                uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));
        state.setHttpClientRequest(httpClientRequest);
        return httpClientRequest;
    }

    private <T> Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest, RestClientRequestContext state)
            throws IOException {
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        Buffer actualEntity = QuarkusRestAsyncInvoker.EMPTY_BUFFER;
        Entity<?> entity = state.getEntity();
        if (entity != null) {
            // no need to set the entity.getMediaType, it comes from the variant
            if (entity.getVariant() != null) {
                Variant v = entity.getVariant();
                headerMap.putSingle(HttpHeaders.CONTENT_TYPE, v.getMediaType().toString());
                if (v.getLanguageString() != null)
                    headerMap.putSingle(HttpHeaders.CONTENT_LANGUAGE, v.getLanguageString());
                if (v.getEncoding() != null)
                    headerMap.putSingle(HttpHeaders.CONTENT_ENCODING, v.getEncoding());
            }

            actualEntity = state.writeEntity(entity, headerMap,
                    state.getConfiguration().getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR));
        }
        // set the Vertx headers after we've run the interceptors because they can modify them
        MultiMap vertxHttpHeaders = httpClientRequest.headers();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            vertxHttpHeaders.add(entry.getKey(), entry.getValue());
        }
        return actualEntity;
    }
}
