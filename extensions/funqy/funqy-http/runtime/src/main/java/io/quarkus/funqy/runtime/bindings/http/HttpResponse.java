package io.quarkus.funqy.runtime.bindings.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private int statusCode;
    private Map<String, String> headers;
    private Object body;

    public HttpResponse(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpResponse(Object body) {
        this.statusCode = 200;
        this.body = body;
    }

    public HttpResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public HttpResponse(int statusCode, Map<String, String> headers, Object body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getBody() {
        return body;
    }

    public boolean hasHeaders() {
        return headers != null;
    }

    public boolean hasBody() {
        return body != null;
    }

    public void addHeaders(String key, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(key, value);
    }
}
