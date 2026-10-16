package io.quarkus.vertx.http.proxy;

import java.util.List;

import jakarta.inject.Singleton;

import io.quarkus.vertx.http.XForwardedForSelector;
import io.vertx.core.MultiMap;

@Singleton
class CustomXForwardedForSelector implements XForwardedForSelector {

    @Override
    public String select(Context context) {
        MultiMap headers = context.headers();
        if (headers.contains("X-Test-Throw")) {
            throw new RuntimeException("selector failure");
        }
        if (headers.contains("X-Test-Reject")) {
            return null;
        }
        if (headers.contains("X-Test-Empty")) {
            return "";
        }
        if (headers.contains("X-Test-Blank")) {
            return "   ";
        }
        if (headers.contains("X-Test-Padded")) {
            return "  1.2.3.4  ";
        }
        if (headers.contains("X-Test-Peer")) {
            return context.connectionPeer().host();
        }
        if (headers.contains("X-Test-Header")) {
            return headers.get("CF-Connecting-IP");
        }
        List<String> values = context.xForwardedForValues();
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }
}
