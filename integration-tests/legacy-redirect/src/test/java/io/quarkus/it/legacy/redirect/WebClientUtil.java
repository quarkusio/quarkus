package io.quarkus.it.legacy.redirect;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class WebClientUtil {

    static class Result {
        String requestedPath;
        int statusCode;
        String redirectPath;
    }

    URL hostPortUrl;

    void setHostPortUrl(URL hostPortUrl) {
        this.hostPortUrl = hostPortUrl;
    }

    public void validate(String requestPath, int expectedStatusCode) {
        internalValidate(requestPath, expectedStatusCode, null, false);
    }

    public void validateText(String requestPath, int expectedStatusCode) {
        internalValidate(requestPath, expectedStatusCode, null, true);
    }

    public void validate(String requestPath, int expectedStatusCode, String expectedRedirect) {
        internalValidate(requestPath, expectedStatusCode, expectedRedirect, false);
    }

    public void validateText(String requestPath, int expectedStatusCode, String expectedRedirect) {
        internalValidate(requestPath, expectedStatusCode, expectedRedirect, false);
    }

    void internalValidate(String requestPath, int expectedStatusCode, String expectedRedirect, boolean text) {
        WebClientUtil.Result result = get(hostPortUrl, requestPath, text);
        Assertions.assertEquals(expectedStatusCode, result.statusCode,
                "Expected status code " + expectedStatusCode + " for request to path " + requestPath);
        if (expectedRedirect != null) {
            Assertions.assertEquals(expectedRedirect, result.redirectPath,
                    "Expected a Location header value of " + expectedRedirect + " for request to path " + requestPath);
        }
    }

    Result get(URL hostPortUrl, final String requestPath, boolean text) {
        Vertx vertx = Vertx.vertx();

        try {
            CompletableFuture<Result> resultFuture = new CompletableFuture<>();

            WebClientOptions options = new WebClientOptions()
                    .setFollowRedirects(false);

            HttpRequest<Buffer> request = WebClient.create(vertx, options)
                    .get(hostPortUrl.getPort(), hostPortUrl.getHost(), requestPath);

            if (text) {
                request.putHeader("Accept", "text/plain");
            }

            request.send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    Result result = new Result();
                    result.requestedPath = requestPath;
                    result.statusCode = response.statusCode();
                    if (result.statusCode == 301) {
                        result.redirectPath = response.getHeader("Location");
                    }
                    resultFuture.complete(result);
                } else {
                    resultFuture.completeExceptionally(ar.cause());
                }
            });

            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Assertions.fail(e);
            return null;
        } finally {
            vertx.close();
        }
    }

}
