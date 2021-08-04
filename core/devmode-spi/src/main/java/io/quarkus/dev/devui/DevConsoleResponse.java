package io.quarkus.dev.devui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevConsoleResponse {

    private int status;
    private Map<String, List<String>> headers = new HashMap<>();
    private byte[] body;

    public DevConsoleResponse(int status, Map<String, List<String>> headers, byte[] body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public DevConsoleResponse() {
        this.status = 200;
        this.headers = new HashMap<>();
        this.body = new byte[0];
    }

    public int getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public DevConsoleResponse setStatus(int status) {
        this.status = status;
        return this;
    }

    public DevConsoleResponse setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    public DevConsoleResponse setBody(byte[] body) {
        this.body = body;
        return this;
    }
}
