package io.quarkus.dev.devui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DevConsoleRequest {

    private final String method;
    private final String uri;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final CompletableFuture<DevConsoleResponse> response = new CompletableFuture<>();

    public DevConsoleRequest(String method, String uri, Map<String, List<String>> headers, byte[] body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public String getUri() {
        return uri;
    }

    public CompletableFuture<DevConsoleResponse> getResponse() {
        return response;
    }

}
