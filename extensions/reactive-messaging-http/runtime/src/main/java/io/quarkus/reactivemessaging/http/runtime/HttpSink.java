package io.quarkus.reactivemessaging.http.runtime;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniRetry;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

class HttpSink {

    private static final Logger log = Logger.getLogger(HttpSink.class);

    private static final String[] SUPPORTED_SCHEMES = { "http:", "https:" };

    private final SubscriberBuilder<Message<?>, Void> subscriber;
    private final WebClient client;
    private final String method;
    private final String url;
    private final SerializerFactoryBase serializerFactory;
    private final String serializerName;

    HttpSink(Vertx vertx, String method, String url,
            String serializerName,
            int maxRetries,
            double jitter,
            Optional<Duration> delay,
            SerializerFactoryBase serializerFactory) {
        this.method = method;
        this.url = url;
        this.serializerFactory = serializerFactory;
        this.serializerName = serializerName;

        client = WebClient.create(io.vertx.mutiny.core.Vertx.newInstance(vertx));

        if (Arrays.stream(SUPPORTED_SCHEMES).noneMatch(url.toLowerCase()::startsWith)) {
            throw new IllegalArgumentException("Unsupported scheme for the http connector in URL: " + url);
        }

        subscriber = ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(m -> {
                    Uni<Void> send = send(m);
                    if (maxRetries > 0) {
                        UniRetry<Void> retry = send.onFailure().retry();
                        if (delay.isPresent()) {
                            retry = retry.withBackOff(delay.get()).withJitter(jitter);
                        }
                        send = retry.atMost(maxRetries);
                    }

                    return send
                            .onItemOrFailure().transformToUni((result, error) -> {
                                if (error != null) {
                                    return Uni.createFrom().completionStage(m.nack(error).thenApply(x -> m));
                                }
                                return Uni.createFrom().completionStage(m.ack().thenApply(x -> m));
                            })
                            .subscribeAsCompletionStage();
                }).ignore();
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }

    private Uni<Void> send(Message<?> message) {
        HttpRequest<?> request = toHttpRequest(message);
        return Uni.createFrom().item(message.getPayload())
                .onItem().transform(this::serialize)
                .onItem().transformToUni(buffer -> invoke(request, buffer));
    }

    private <T> Buffer serialize(T payload) {
        Serializer<T> serializer = serializerFactory.getSerializer(serializerName, payload);
        return Buffer.newInstance(serializer.serialize(payload));
    }

    private Uni<Void> invoke(HttpRequest<?> request, Buffer buffer) {
        return request
                .sendBuffer(buffer)
                .onItem().transform(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return null;
                    } else {
                        throw new RuntimeException("HTTP request returned an invalid status: " + resp.statusCode());
                    }
                });
    }

    private HttpRequest<?> toHttpRequest(Message<?> message) {
        try {
            OutgoingHttpMetadata metadata = message.getMetadata(OutgoingHttpMetadata.class).orElse((OutgoingHttpMetadata) null);

            Map<String, List<String>> httpHeaders = metadata != null ? metadata.getHeaders() : Collections.emptyMap();
            Map<String, List<String>> query = metadata != null ? metadata.getQuery() : Collections.emptyMap();
            Map<String, String> pathParams = metadata != null ? metadata.getPathParameters() : Collections.emptyMap();

            String url = prepareUrl(pathParams);

            HttpRequest<Buffer> request = createRequest(url);

            addHeaders(request, httpHeaders);

            addQueryParameters(query, request);

            return request;
        } catch (Exception any) {
            log.error("Failed to transform message to http request", any);
            throw any;
        }
    }

    private HttpRequest<Buffer> createRequest(String url) {
        HttpRequest<Buffer> request;
        switch (method) {
            case "POST":
                request = client.postAbs(url);
                break;
            case "PUT":
                request = client.putAbs(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method + "only PUT and POST are supported");
        }
        return request;
    }

    private void addQueryParameters(Map<String, List<String>> query, HttpRequest<Buffer> request) {
        for (Map.Entry<String, List<String>> queryParam : query.entrySet()) {
            for (String queryParamValue : queryParam.getValue()) {
                request.addQueryParam(queryParam.getKey(), queryParamValue);
            }
        }
    }

    private void addHeaders(HttpRequest<Buffer> request, Map<String, List<String>> httpHeaders) {
        if (!httpHeaders.isEmpty()) {
            for (Map.Entry<String, List<String>> header : httpHeaders.entrySet()) {
                request.putHeader(header.getKey(), header.getValue());
            }
        }
    }

    private String prepareUrl(Map<String, String> pathParams) {
        String result = url;
        for (Map.Entry<String, String> pathParamEntry : pathParams.entrySet()) {
            String toReplace = String.format("{%s}", pathParamEntry.getKey());
            if (url.contains(toReplace)) {
                result = url.replace(toReplace, pathParamEntry.getValue());
            } else {
                log.warnf("Failed to find %s in the URL that would correspond to the %s path parameter",
                        toReplace, pathParamEntry.getKey());
            }
        }

        return result;
    }
}
