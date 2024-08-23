package io.quarkus.rest.client.reactive.registerclientheaders;

import java.util.List;
import java.util.Map;

public class RequestData {
    private Map<String, List<String>> headers;

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
}
