package org.jboss.resteasy.reactive.client.handlers;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.streams.Pipe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.AsyncResultUni;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;

public class ClientSendRequestHandler implements ClientRestHandler {
    private static final Logger log = Logger.getLogger(ClientSendRequestHandler.class);

    private final boolean followRedirects;

    public ClientSendRequestHandler(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) {
        if (requestContext.isAborted()) {
            return;
        }
        requestContext.suspend();
        Uni<HttpClientRequest> future = createRequest(requestContext);

        // DNS failures happen before we send the request
        future.subscribe().with(new Consumer<>() {
            @Override
            public void accept(HttpClientRequest httpClientRequest) {
                final long startTime;

                if (requestContext.getCallStatsCollector() != null) {
                    startTime = System.nanoTime();
                } else {
                    startTime = 0L;
                }
                Future<HttpClientResponse> sent;
                if (requestContext.isMultipart()) {
                    Promise<HttpClientRequest> requestPromise = Promise.promise();
                    QuarkusMultipartFormUpload actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this.setMultipartHeadersAndPrepareBody(httpClientRequest,
                                requestContext);

                        Pipe<Buffer> pipe = actualEntity.pipe(); // Shouldn't this be called in an earlier phase ?
                        requestPromise.future().onComplete(ar -> {
                            if (ar.succeeded()) {
                                HttpClientRequest req = ar.result();
                                if (httpClientRequest.headers() == null
                                        || !httpClientRequest.headers().contains(HttpHeaders.CONTENT_LENGTH)) {
                                    req.setChunked(true);
                                }
                                pipe.endOnFailure(false);
                                pipe.to(req, ar2 -> {
                                    if (ar2.failed()) {
                                        req.reset(0L, ar2.cause());
                                    }
                                });
                                actualEntity.run();
                            } else {
                                pipe.close();
                            }
                        });
                        sent = httpClientRequest.response();

                        requestPromise.complete(httpClientRequest);
                    } catch (Throwable e) {
                        reportFinish(System.nanoTime() - startTime, e, requestContext);
                        requestContext.resume(e);
                        return;
                    }
                } else {
                    Buffer actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this
                                .setRequestHeadersAndPrepareBody(httpClientRequest, requestContext);
                    } catch (Throwable e) {
                        requestContext.resume(e);
                        return;
                    }
                    if (actualEntity == AsyncInvokerImpl.EMPTY_BUFFER) {
                        sent = httpClientRequest.send();
                    } else {
                        sent = httpClientRequest.send(actualEntity);
                    }
                }

                sent.onSuccess(new Handler<>() {
                    @Override
                    public void handle(HttpClientResponse clientResponse) {
                        try {
                            requestContext.initialiseResponse(clientResponse);
                            int status = clientResponse.statusCode();
                            if (status >= 500 && status < 600) {
                                reportFinish(System.nanoTime() - startTime, new InternalServerErrorException(), requestContext);
                            } else {
                                reportFinish(System.nanoTime() - startTime, null, requestContext);
                            }
                            if (!requestContext.isRegisterBodyHandler()) {
                                clientResponse.pause();
                                requestContext.resume();
                            } else {
                                clientResponse.bodyHandler(new Handler<>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            if (buffer.length() > 0) {
                                                requestContext.setResponseEntityStream(
                                                        new ByteArrayInputStream(buffer.getBytes()));
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
                            reportFinish(System.nanoTime() - startTime, t, requestContext);
                            requestContext.resume(t);
                        }
                    }
                })
                        .onFailure(new Handler<>() {
                            @Override
                            public void handle(Throwable failure) {
                                if (failure instanceof IOException) {
                                    requestContext.resume(new ProcessingException(failure));
                                } else {
                                    requestContext.resume(failure);
                                }
                            }
                        });
            }
        }, new Consumer<>() {
            @Override
            public void accept(Throwable event) {
                if (event instanceof IOException) {
                    ProcessingException throwable = new ProcessingException(event);
                    reportFinish(0, throwable, requestContext);
                    requestContext.resume(throwable);
                } else {
                    requestContext.resume(event);
                    reportFinish(0, event, requestContext);
                }
            }
        });
    }

    private void reportFinish(long timeInNs, Throwable throwable, RestClientRequestContext requestContext) {
        ServiceInstance serviceInstance = requestContext.getCallStatsCollector();
        if (serviceInstance != null) {
            serviceInstance.recordResult(timeInNs, throwable);
        }
    }

    public Uni<HttpClientRequest> createRequest(RestClientRequestContext state) {
        HttpClient httpClient = state.getHttpClient();
        URI uri = state.getUri();
        Object readTimeout = state.getConfiguration().getProperty(QuarkusRestClientProperties.READ_TIMEOUT);
        Uni<RequestOptions> requestOptions;
        if (uri.getScheme().startsWith(Stork.STORK)) {
            boolean isHttps = "storks".equals(uri.getScheme());
            String serviceName = uri.getHost();
            Uni<ServiceInstance> serviceInstance;
            try {
                serviceInstance = Stork.getInstance()
                        .getService(serviceName)
                        .selectServiceInstance();
            } catch (Throwable e) {
                log.error("Error selecting service instance for serviceName: " + serviceName, e);
                return Uni.createFrom().failure(e);
            }
            requestOptions = serviceInstance.onItem().transform(new Function<>() {
                @Override
                public RequestOptions apply(ServiceInstance serviceInstance) {
                    if (serviceInstance.gatherStatistics()) {
                        state.setCallStatsCollector(serviceInstance);
                    }
                    return new RequestOptions()
                            .setHost(serviceInstance.getHost())
                            .setPort(serviceInstance.getPort())
                            .setSsl(isHttps);
                }
            });
        } else {
            boolean isHttps = "https".equals(uri.getScheme());
            int port = getPort(isHttps, uri.getPort());
            requestOptions = Uni.createFrom().item(new RequestOptions().setHost(uri.getHost())
                    .setPort(port).setSsl(isHttps));
        }

        return requestOptions.onItem()
                .transform(r -> r.setMethod(HttpMethod.valueOf(state.getHttpMethod()))
                        .setURI(uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()))
                        .setFollowRedirects(followRedirects))
                .onItem().invoke(r -> {
                    if (readTimeout instanceof Long) {
                        r.setTimeout((Long) readTimeout);
                    }
                })
                .onItem().transformToUni(new Function<RequestOptions, Uni<? extends HttpClientRequest>>() {
                    @Override
                    public Uni<? extends HttpClientRequest> apply(RequestOptions options) {
                        return AsyncResultUni.toUni(handler -> httpClient.request(options, handler));
                    }
                });
    }

    private int getPort(boolean isHttps, int specifiedPort) {
        return specifiedPort != -1 ? specifiedPort : defaultPort(isHttps);
    }

    private int defaultPort(boolean isHttps) {
        return isHttps ? 443 : 80;
    }

    private QuarkusMultipartFormUpload setMultipartHeadersAndPrepareBody(HttpClientRequest httpClientRequest,
            RestClientRequestContext state) throws Exception {
        if (!(state.getEntity().getEntity() instanceof QuarkusMultipartForm)) {
            throw new IllegalArgumentException(
                    "Multipart form upload expects an entity of type MultipartForm, got: " + state.getEntity().getEntity());
        }
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        QuarkusMultipartForm multipartForm = (QuarkusMultipartForm) state.getEntity().getEntity();
        multipartForm.preparePojos(state);

        Object property = state.getConfiguration().getProperty(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE);
        HttpPostRequestEncoder.EncoderMode mode = HttpPostRequestEncoder.EncoderMode.RFC1738;
        if (property != null) {
            mode = (HttpPostRequestEncoder.EncoderMode) property;
        }
        QuarkusMultipartFormUpload multipartFormUpload = new QuarkusMultipartFormUpload(Vertx.currentContext(), multipartForm,
                true,
                mode);
        setEntityRelatedHeaders(headerMap, state.getEntity());

        // multipart has its own headers:
        MultiMap multipartHeaders = multipartFormUpload.headers();
        for (String multipartHeader : multipartHeaders.names()) {
            headerMap.put(multipartHeader, multipartHeaders.getAll(multipartHeader));
        }

        setVertxHeaders(httpClientRequest, headerMap);
        return multipartFormUpload;
    }

    private Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest,
            RestClientRequestContext state)
            throws IOException {
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        Buffer actualEntity = AsyncInvokerImpl.EMPTY_BUFFER;
        Entity<?> entity = state.getEntity();
        if (entity != null) {
            // no need to set the entity.getMediaType, it comes from the variant
            setEntityRelatedHeaders(headerMap, entity);

            actualEntity = state.writeEntity(entity, headerMap,
                    state.getConfiguration().getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR));
        }
        // set the Vertx headers after we've run the interceptors because they can modify them
        setVertxHeaders(httpClientRequest, headerMap);
        return actualEntity;
    }

    private void setVertxHeaders(HttpClientRequest httpClientRequest, MultivaluedMap<String, String> headerMap) {
        MultiMap vertxHttpHeaders = httpClientRequest.headers();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            vertxHttpHeaders.add(entry.getKey(), entry.getValue());
        }
    }

    private void setEntityRelatedHeaders(MultivaluedMap<String, String> headerMap, Entity<?> entity) {
        if (entity.getVariant() != null) {
            Variant v = entity.getVariant();
            headerMap.putSingle(HttpHeaders.CONTENT_TYPE, v.getMediaType().toString());
            if (v.getLanguageString() != null) {
                headerMap.putSingle(HttpHeaders.CONTENT_LANGUAGE, v.getLanguageString());
            }
            if (v.getEncoding() != null) {
                headerMap.putSingle(HttpHeaders.CONTENT_ENCODING, v.getEncoding());
            }
        }
    }
}
