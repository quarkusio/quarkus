package io.quarkus.security.webauthn.impl;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletionStage;

import com.webauthn4j.async.metadata.HttpAsyncClient;
import com.webauthn4j.metadata.HttpClient.Response;
import com.webauthn4j.metadata.exception.MDSException;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.impl.http.SimpleHttpClient;

public class VertxHttpAsyncClient implements HttpAsyncClient {

    private static final byte[] NO_BYTES = new byte[0];
    private SimpleHttpClient httpClient;

    public VertxHttpAsyncClient(Vertx vertx) {
        this.httpClient = new SimpleHttpClient(vertx, "vertx-auth", new HttpClientOptions());
    }

    @Override
    public CompletionStage<Response> fetch(String uri) throws MDSException {
        return httpClient
                .fetch(HttpMethod.GET, uri, null, null)
                .map(res -> {
                    Buffer body = res.body();
                    byte[] bytes;
                    if (body != null) {
                        bytes = body.getBytes();
                    } else {
                        bytes = NO_BYTES;
                    }
                    return new Response(res.statusCode(), new ByteArrayInputStream(bytes));
                }).toCompletionStage();
    }

}
