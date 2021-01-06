package io.quarkus.dev.console;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DevConsoleRequest {

    private final String method;
    private final String path;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final CompletableFuture<DevConsoleResponse> response = new CompletableFuture<>();

    public DevConsoleRequest(String method, String path, Map<String, List<String>> headers, byte[] body) {
        this.method = method;
        this.path = path;
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

    public String getPath() {
        return path;
    }

    public CompletableFuture<DevConsoleResponse> getResponse() {
        return response;
    }

}
