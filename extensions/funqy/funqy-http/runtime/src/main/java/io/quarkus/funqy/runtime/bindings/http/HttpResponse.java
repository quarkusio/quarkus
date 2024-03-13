package io.quarkus.funqy.runtime.bindings.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private final int statusCode;
    private final Object body;

    public HttpResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getBody() {
        return body;
    }

}
