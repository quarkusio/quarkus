package org.jboss.resteasy.reactive.client.handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.AsyncResultUni;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartResponseDecoder;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.Pump;

public class ClientSendRequestHandler implements ClientRestHandler {
    private static final Logger log = Logger.getLogger(ClientSendRequestHandler.class);
    public static final String CONTENT_TYPE = "Content-Type";

    private final boolean followRedirects;
    private final LoggingScope loggingScope;
    private final ClientLogger clientLogger;
    private final Map<Class<?>, MultipartResponseData> multipartResponseDataMap;
    private final int maxChunkSize;

    public ClientSendRequestHandler(int maxChunkSize, boolean followRedirects, LoggingScope loggingScope, ClientLogger logger,
            Map<Class<?>, MultipartResponseData> multipartResponseDataMap) {
        this.maxChunkSize = maxChunkSize;
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
        Uni<HttpClientRequest> future = createRequest(requestContext);

        // DNS failures happen before we send the request
        future.subscribe().with(new Consumer<>() {
            @Override
            public void accept(HttpClientRequest httpClientRequest) {
                // adapt headers to HTTP/2 depending on the underlying HTTP connection
                ClientSendRequestHandler.this.adaptRequest(httpClientRequest);

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
                        Future<HttpClientResponse> sent = httpClientRequest.response();
                        requestPromise.complete(httpClientRequest);
                        attachSentHandlers(sent, httpClientRequest, requestContext);
                    } catch (Throwable e) {
                        reportFinish(e, requestContext);
                        requestContext.resume(e);
                        return;
                    }
                } else if (requestContext.isFileUpload()) {
                    Vertx vertx = Vertx.currentContext().owner();
                    Object entity = requestContext.getEntity().getEntity();
                    String filePathToUpload = null;
                    if (entity instanceof File) {
                        filePathToUpload = ((File) entity).getAbsolutePath();
                    } else if (entity instanceof Path) {
                        filePathToUpload = ((Path) entity).toAbsolutePath().toString();
                    }
                    vertx.fileSystem()
                            .open(filePathToUpload, new OpenOptions().setRead(true).setWrite(false),
                                    new Handler<>() {
                                        @Override
                                        public void handle(AsyncResult<AsyncFile> openedAsyncFile) {
                                            if (openedAsyncFile.failed()) {
                                                requestContext.resume(openedAsyncFile.cause());
                                                return;
                                            }

                                            MultivaluedMap<String, String> headerMap = requestContext.getRequestHeaders()
                                                    .asMap();
                                            updateRequestHeadersFromConfig(requestContext, headerMap);

                                            // set the Vertx headers after we've run the interceptors because they can modify them
                                            setVertxHeaders(httpClientRequest, headerMap);

                                            Future<HttpClientResponse> sent = httpClientRequest.send(openedAsyncFile.result());
                                            attachSentHandlers(sent, httpClientRequest, requestContext);
                                        }
                                    });
                } else {
                    Future<HttpClientResponse> sent;
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
                    attachSentHandlers(sent, httpClientRequest, requestContext);
                }
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

    private void attachSentHandlers(Future<HttpClientResponse> sent,
            HttpClientRequest httpClientRequest,
            RestClientRequestContext requestContext) {
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
                    } else if (!requestContext.isRegisterBodyHandler()
                            && (Response.Status.Family.familyOf(status) == Response.Status.Family.SUCCESSFUL)) { // we force the registration of a body handler if there was an error, so we can ensure the body can be read
                        clientResponse.pause();
                        if (loggingScope != LoggingScope.NONE) {
                            clientLogger.logResponse(clientResponse, false);
                        }
                        requestContext.resume();
                    } else {
                        if (requestContext.isFileDownload()) {
                            // when downloading a file we copy the bytes to the file system and manually set the entity type
                            // this is needed because large files can cause OOM or exceed the InputStream limit (of 2GB)

                            clientResponse.pause();
                            Vertx vertx = Vertx.currentContext().owner();
                            vertx.fileSystem().createTempFile("rest-client", "",
                                    new Handler<>() {
                                        @Override
                                        public void handle(AsyncResult<String> tempFileCreation) {
                                            if (tempFileCreation.failed()) {
                                                reportFinish(tempFileCreation.cause(), requestContext);
                                                requestContext.resume(tempFileCreation.cause());
                                                return;
                                            }
                                            String tmpFilePath = tempFileCreation.result();
                                            vertx.fileSystem().open(tmpFilePath,
                                                    new OpenOptions().setWrite(true),
                                                    new Handler<>() {
                                                        @Override
                                                        public void handle(AsyncResult<AsyncFile> asyncFileOpened) {
                                                            if (asyncFileOpened.failed()) {
                                                                reportFinish(asyncFileOpened.cause(), requestContext);
                                                                requestContext.resume(asyncFileOpened.cause());
                                                                return;
                                                            }
                                                            final AsyncFile tmpAsyncFile = asyncFileOpened.result();
                                                            final Pump downloadPump = Pump.pump(clientResponse,
                                                                    tmpAsyncFile);
                                                            downloadPump.start();

                                                            clientResponse.resume();
                                                            clientResponse.endHandler(new Handler<>() {
                                                                public void handle(Void event) {
                                                                    tmpAsyncFile.flush(new Handler<>() {
                                                                        public void handle(AsyncResult<Void> flushed) {
                                                                            if (flushed.failed()) {
                                                                                reportFinish(flushed.cause(),
                                                                                        requestContext);
                                                                                requestContext.resume(flushed.cause());
                                                                                return;
                                                                            }

                                                                            if (loggingScope != LoggingScope.NONE) {
                                                                                clientLogger.logRequest(
                                                                                        httpClientRequest, null, false);
                                                                            }

                                                                            requestContext.setTmpFilePath(tmpFilePath);
                                                                            requestContext.resume();
                                                                        }
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    });
                                        }
                                    });

                        } else if (requestContext.isInputStreamDownload()) {
                            if (loggingScope != LoggingScope.NONE) {
                                clientLogger.logResponse(clientResponse, false);
                            }
                            //TODO: make timeout configureable
                            requestContext
                                    .setResponseEntityStream(
                                            new VertxClientInputStream(clientResponse, 100000, requestContext));
                            requestContext.resume();
                        } else {
                            clientResponse.body(new Handler<>() {
                                @Override
                                public void handle(AsyncResult<Buffer> ar) {
                                    if (ar.succeeded()) {
                                        if (loggingScope != LoggingScope.NONE) {
                                            clientLogger.logResponse(clientResponse, false);
                                        }
                                        Buffer buffer = ar.result();
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
                                    } else {
                                        requestContext.resume(ar.cause());
                                    }
                                }
                            });
                        }
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
                        if (failure instanceof HttpClosedException) {
                            // This is because of the Rest Client TCK
                            // HttpClosedException is a runtime exception. If we complete with that exception, it gets
                            // unwrapped by the rest client proxy and thus fails the TCK.
                            // By creating an IOException, we avoid that and provide a meaningful exception (because
                            // it's an I/O exception)
                            requestContext.resume(new ProcessingException(new IOException(failure.getMessage())));
                        } else if (failure instanceof IOException) {
                            requestContext.resume(new ProcessingException(failure));
                        } else {
                            requestContext.resume(failure);
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

        return requestOptions.onItem()
                .transform(r -> r.setMethod(HttpMethod.valueOf(state.getHttpMethod()))
                        .setURI(uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()))
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

    private boolean shouldMeasureTime(RestClientRequestContext state) {
        return !Multi.class.equals(state.getResponseType().getRawType());
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
                true, maxChunkSize, mode);
        httpClientRequest.setChunked(multipartFormUpload.isChunked());
        setEntityRelatedHeaders(headerMap, state.getEntity());

        // multipart has its own headers:
        MultiMap multipartHeaders = multipartFormUpload.headers();
        for (String multipartHeader : multipartHeaders.names()) {
            headerMap.put(multipartHeader, multipartHeaders.getAll(multipartHeader));
        }

        if (state.getEntity().getVariant() != null) {
            Variant v = state.getEntity().getVariant();
            String variantContentType = v.getMediaType().toString();
            String multipartContentType = headerMap.getFirst(HttpHeaders.CONTENT_TYPE);
            if (multipartContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())
                    && !variantContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
                // this is a total hack whose purpose is to allow the @Consumes annotation to override the media type
                int semicolonIndex = multipartContentType.indexOf(';');
                headerMap.put(HttpHeaders.CONTENT_TYPE,
                        List.of(variantContentType + multipartContentType.substring(semicolonIndex)));
            }
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

    private void adaptRequest(HttpClientRequest request) {
        if (request.version() == HttpVersion.HTTP_2) {
            // When using the protocol HTTP/2, Netty which is internally used by Vert.x will validate the headers and reject
            // the requests with invalid metadata.
            // When we start a new connection, the Vert.x client will automatically upgrade the first request we make to be
            // valid in HTTP/2.
            // The problem is that in next requests, the Vert.x client reuses the same connection within the same window time
            // and hence does not upgrade the following requests. Therefore, even though the first request works fine, the
            // next requests won't work.
            // This has been reported in https://github.com/eclipse-vertx/vert.x/issues/4618.
            // To workaround this issue, we need to "upgrade" the next requests by ourselves when the version is already set
            // to HTTP/2:
            if (request.path() == null || request.path().length() == 0) {
                // HTTP/2 does not allow empty paths
                request.setURI(request.getURI() + "/");
            }
        }
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
            if (!headerMap.containsKey(HttpHeaders.CONTENT_TYPE)) {
                headerMap.putSingle(HttpHeaders.CONTENT_TYPE, v.getMediaType().toString());
            }
            if ((v.getLanguageString() != null) && !headerMap.containsKey(HttpHeaders.CONTENT_LANGUAGE)) {
                headerMap.putSingle(HttpHeaders.CONTENT_LANGUAGE, v.getLanguageString());
            }
            if ((v.getEncoding() != null) && !headerMap.containsKey(HttpHeaders.CONTENT_ENCODING)) {
                headerMap.putSingle(HttpHeaders.CONTENT_ENCODING, v.getEncoding());
            }
        }
    }
}
