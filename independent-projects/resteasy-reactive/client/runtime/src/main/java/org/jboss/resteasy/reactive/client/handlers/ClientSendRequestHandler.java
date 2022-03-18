package org.jboss.resteasy.reactive.client.handlers;

import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.Context;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.AsyncResultUni;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartResponseDecoder;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

public class ClientSendRequestHandler implements ClientRestHandler {
    private static final Logger log = Logger.getLogger(ClientSendRequestHandler.class);
    public static final String CONTENT_TYPE = "Content-Type";

    private final boolean followRedirects;
    private final LoggingScope loggingScope;
    private final ClientLogger clientLogger;
    private final Map<Class<?>, MultipartResponseData> multipartResponseDataMap;

    public ClientSendRequestHandler(boolean followRedirects, LoggingScope loggingScope, ClientLogger logger,
            Map<Class<?>, MultipartResponseData> multipartResponseDataMap) {
        this.followRedirects = followRedirects;
        this.loggingScope = loggingScope;
        this.clientLogger = logger;
        this.multipartResponseDataMap = multipartResponseDataMap;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) {
        if (requestContext.isAborted()) {
            return;
        }
        requestContext.suspend();
        Uni<HttpClientRequest> future = createRequest(requestContext)
                .runSubscriptionOn(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        Context current = Vertx.currentContext();
                        ClientRequestContextImpl clientRequestContext = requestContext.getClientRequestContext();
                        Context captured = null;
                        if (clientRequestContext != null) {
                            captured = clientRequestContext.getContext();
                        }
                        if (current == captured || captured == null) {
                            // No need to switch to another context.
                            command.run();
                        } else {
                            // Switch back to the captured context
                            captured.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void ignored) {
                                    command.run();
                                }
                            });
                        }
                    }
                });

        // DNS failures happen before we send the request
        future.subscribe().with(new Consumer<>() {
            @Override
            public void accept(HttpClientRequest httpClientRequest) {
                Future<HttpClientResponse> sent;
                if (requestContext.isMultipart()) {
                    Promise<HttpClientRequest> requestPromise = Promise.promise();
                    QuarkusMultipartFormUpload actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this.setMultipartHeadersAndPrepareBody(httpClientRequest,
                                requestContext);
                        if (loggingScope != LoggingScope.NONE) {
                            clientLogger.logRequest(httpClientRequest, null, true);
                        }

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
                        reportFinish(e, requestContext);
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
                        if (loggingScope != LoggingScope.NONE) {
                            clientLogger.logRequest(httpClientRequest, null, false);
                        }
                    } else {
                        sent = httpClientRequest.send(actualEntity);
                        if (loggingScope != LoggingScope.NONE) {
                            clientLogger.logRequest(httpClientRequest, actualEntity, false);
                        }
                    }
                }

                sent.onSuccess(new Handler<>() {
                    @Override
                    public void handle(HttpClientResponse clientResponse) {
                        try {
                            requestContext.initialiseResponse(clientResponse);
                            int status = clientResponse.statusCode();
                            if (requestContext.getCallStatsCollector() != null) {
                                if (status >= 500 && status < 600) {
                                    reportFinish(new InternalServerErrorException(),
                                            requestContext);
                                } else {
                                    reportFinish(null, requestContext);
                                }
                            }

                            if (isResponseMultipart(requestContext)) {
                                QuarkusMultipartResponseDecoder multipartDecoder = new QuarkusMultipartResponseDecoder(
                                        clientResponse);

                                clientResponse.handler(multipartDecoder::offer);

                                clientResponse.endHandler(new Handler<>() {
                                    @Override
                                    public void handle(Void event) {
                                        multipartDecoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);

                                        List<InterfaceHttpData> datas = multipartDecoder.getBodyHttpDatas();
                                        requestContext.setResponseMultipartParts(datas);

                                        if (loggingScope != LoggingScope.NONE) {
                                            clientLogger.logResponse(clientResponse, false);
                                        }

                                        requestContext.resume();
                                    }
                                });
                            } else if (!requestContext.isRegisterBodyHandler()) {
                                clientResponse.pause();
                                if (loggingScope != LoggingScope.NONE) {
                                    clientLogger.logResponse(clientResponse, false);
                                }
                                requestContext.resume();
                            } else {
                                clientResponse.bodyHandler(new Handler<>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        if (loggingScope != LoggingScope.NONE) {
                                            clientLogger.logResponse(clientResponse, false);
                                        }
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
                            reportFinish(t, requestContext);
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
                // set some properties to prevent NPEs down the chain
                requestContext.setResponseHeaders(new MultivaluedTreeMap<>());
                requestContext.setResponseReasonPhrase("unknown");

                if (event instanceof IOException) {
                    ProcessingException throwable = new ProcessingException(event);
                    reportFinish(throwable, requestContext);
                    requestContext.resume(throwable);
                } else {
                    requestContext.resume(event);
                    reportFinish(event, requestContext);
                }
            }
        });
    }

    private boolean isResponseMultipart(RestClientRequestContext requestContext) {
        MultivaluedMap<String, String> responseHeaders = requestContext.getResponseHeaders();
        List<String> contentTypes = responseHeaders.get(CONTENT_TYPE);
        if (contentTypes != null) {
            for (String contentType : contentTypes) {
                if (contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.MULTIPART_FORM_DATA)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reportFinish(Throwable throwable, RestClientRequestContext requestContext) {
        ServiceInstance serviceInstance = requestContext.getCallStatsCollector();
        if (serviceInstance != null) {
            serviceInstance.recordReply();
            serviceInstance.recordEnd(throwable);
        }
    }

    public Uni<HttpClientRequest> createRequest(RestClientRequestContext state) {
        HttpClient httpClient = state.getHttpClient();
        URI uri = state.getUri();
        Object readTimeout = state.getConfiguration().getProperty(QuarkusRestClientProperties.READ_TIMEOUT);
        Uni<RequestOptions> requestOptions;
        state.setMultipartResponsesData(multipartResponseDataMap);
        if (uri.getScheme() == null) { // invalid URI
            return Uni.createFrom()
                    .failure(new IllegalArgumentException("Invalid REST Client URL used: '" + uri + "'"));
        }
        if (uri.getScheme().startsWith(Stork.STORK)) {
            boolean isHttps = "storks".equals(uri.getScheme());
            String serviceName = uri.getHost();
            if (serviceName == null) { // invalid URI
                return Uni.createFrom()
                        .failure(new IllegalArgumentException("Invalid REST Client URL used: '" + uri + "'"));
            }
            Uni<ServiceInstance> serviceInstance;
            try {
                serviceInstance = Stork.getInstance()
                        .getService(serviceName)
                        .selectInstanceAndRecordStart(true);
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
            try {
                URL ignored = uri.toURL();
            } catch (MalformedURLException mue) {
                log.error("Invalid REST Client URL used: '" + uri + "'");
                return Uni.createFrom()
                        .failure(new IllegalArgumentException("Invalid REST Client URL used: '" + uri + "'"));
            }

            boolean isHttps = "https".equals(uri.getScheme());
            int port = getPort(isHttps, uri.getPort());
            requestOptions = Uni.createFrom().item(new RequestOptions().setHost(uri.getHost())
                    .setPort(port).setSsl(isHttps));
        }

        return requestOptions.onItem()
                .transform(r -> r.setMethod(HttpMethod.valueOf(state.getHttpMethod()))
                        .setURI(uri.getPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()))
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
        updateRequestHeadersFromConfig(state, headerMap);
        QuarkusMultipartForm multipartForm = (QuarkusMultipartForm) state.getEntity().getEntity();
        multipartForm.preparePojos(state);

        Object property = state.getConfiguration().getProperty(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE);
        PausableHttpPostRequestEncoder.EncoderMode mode = PausableHttpPostRequestEncoder.EncoderMode.RFC1738;
        if (property != null) {
            mode = (PausableHttpPostRequestEncoder.EncoderMode) property;
        }
        QuarkusMultipartFormUpload multipartFormUpload = new QuarkusMultipartFormUpload(Vertx.currentContext(), multipartForm,
                true, mode);
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
        updateRequestHeadersFromConfig(state, headerMap);

        Buffer actualEntity = AsyncInvokerImpl.EMPTY_BUFFER;
        Entity<?> entity = state.getEntity();
        if (entity != null) {
            // no need to set the entity.getMediaType, it comes from the variant
            setEntityRelatedHeaders(headerMap, entity);

            actualEntity = state.writeEntity(entity, headerMap,
                    state.getConfiguration().getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR));
        } else {
            // some servers don't like the fact that a POST or PUT does not have a method body if there is no content-length header associated
            if (state.getHttpMethod().equals("POST") || state.getHttpMethod().equals("PUT")) {
                headerMap.putSingle(HttpHeaders.CONTENT_LENGTH, "0");
            }
        }
        // set the Vertx headers after we've run the interceptors because they can modify them
        setVertxHeaders(httpClientRequest, headerMap);
        return actualEntity;
    }

    private void updateRequestHeadersFromConfig(RestClientRequestContext state, MultivaluedMap<String, String> headerMap) {
        Object staticHeaders = state.getConfiguration().getProperty(QuarkusRestClientProperties.STATIC_HEADERS);
        if (staticHeaders instanceof Map) {
            for (Map.Entry<String, String> entry : ((Map<String, String>) staticHeaders).entrySet()) {
                headerMap.putSingle(entry.getKey(), entry.getValue());
            }
        }
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
