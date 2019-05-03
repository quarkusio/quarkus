package io.quarkus.amazon.lambda.resteasy.runtime.container;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;

class AwsProxyResteasyRequest extends AwsProxyRequest {
    private Map<String, String> headers = new TreeMap<>();

    public Map<String, String> getHeaders() {

        throw new UnsupportedOperationException("shouldn't be called");
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
        for (final Entry<String, String> entry : headers.entrySet()) {
            getMultiValueHeaders().putSingle(entry.getKey(), entry.getValue());
        }
    }
}
