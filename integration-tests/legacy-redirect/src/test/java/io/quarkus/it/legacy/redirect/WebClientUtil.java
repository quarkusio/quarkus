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
        String contentType;

        public String toString() {
            return "requestedPath=" + requestedPath + ", statusCode=" + statusCode + ", redirectPath=" + redirectPath
                    + ", contentType=" + contentType;
        }
    }

    URL hostPortUrl;

    void setHostPortUrl(URL hostPortUrl) {
        this.hostPortUrl = hostPortUrl;
    }

    public void validate(String requestPath, int expectedStatusCode) {
        internalValidate(requestPath, expectedStatusCode, null, null, false);
    }

    public void validate(String requestPath, int expectedStatusCode, String expectedRedirect) {
        internalValidate(requestPath, expectedStatusCode, expectedRedirect, null, false);
    }

    public void validateText(String requestPath, int expectedStatusCode) {
        internalValidate(requestPath, expectedStatusCode, null, "text/plain", false);
    }

    public void validateText(String requestPath, int expectedStatusCode, String expectedRedirect) {
        internalValidate(requestPath, expectedStatusCode, expectedRedirect, "text/plain", false);
    }

    public void validateContentType(String requestPath, int expectedStatusCode, String contentType) {
        WebClientUtil.Result result = internalValidate(requestPath, expectedStatusCode, null, null, false);
        Assertions.assertTrue(result.contentType.startsWith(contentType),
                "Expected Content-Type for request to path " + requestPath + " to start with " + contentType
                        + ", instead found " + result.contentType);
    }

    public void followForContentType(String requestPath, int expectedStatusCode, String contentType) {
        WebClientUtil.Result result = internalValidate(requestPath, expectedStatusCode, null, null, true);
        Assertions.assertTrue(result.contentType.startsWith(contentType),
                "Expected Content-Type for request to path " + requestPath + " to start with " + contentType
                        + ", instead found " + result.contentType);
    }

    WebClientUtil.Result internalValidate(String requestPath, int expectedStatusCode, String expectedRedirect,
            String acceptType, boolean follow) {

        WebClientUtil.Result result = get(requestPath, acceptType, follow);

        Assertions.assertEquals(expectedStatusCode, result.statusCode,
                "Expected status code " + expectedStatusCode + " for request to path " + requestPath);

        if (expectedRedirect != null) {
            Assertions.assertEquals(expectedRedirect, result.redirectPath,
                    "Expected a Location header value of " + expectedRedirect + " for request to path " + requestPath);
        }

        return result;
    }

    Result get(final String requestPath, final String acceptType, boolean follow) {
        Vertx vertx = Vertx.vertx();

        try {
            CompletableFuture<Result> resultFuture = new CompletableFuture<>();

            WebClientOptions options = new WebClientOptions()
                    .setFollowRedirects(follow);

            HttpRequest<Buffer> request = WebClient.create(vertx, options)
                    .get(hostPortUrl.getPort(), hostPortUrl.getHost(), requestPath);

            if (acceptType != null) {
                request.putHeader("Accept", acceptType);
            }

            request.send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    Result result = new Result();
                    result.requestedPath = requestPath;
                    result.statusCode = response.statusCode();
                    result.contentType = response.getHeader("Content-Type");
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
