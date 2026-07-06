package io.quarkus.vertx.http.runtime.filters.accesslog;

import io.vertx.core.buffer.Buffer;

public final class AccessLogResponseBodyCapture {

    private final int maxSize;
    private final Buffer captured = Buffer.buffer();
    private boolean nonBufferBody;
    private boolean truncated;

    public AccessLogResponseBodyCapture(int maxSize) {
        this.maxSize = maxSize;
    }

    public void capture(Buffer data) {
        if (nonBufferBody || data == null || data.length() == 0) {
            return;
        }
        if (captured.length() >= maxSize) {
            truncated = true;
            return;
        }
        int remaining = maxSize - captured.length();
        int toCapture = Math.min(data.length(), remaining);
        captured.appendBytes(data.getBytes(0, toCapture));
        if (data.length() > remaining) {
            truncated = true;
        }
    }

    public void capture(String data) {
        if (data != null) {
            capture(Buffer.buffer(data));
        }
    }

    public void markNonBufferBody() {
        nonBufferBody = true;
    }

    public String getCapturedBody() {
        if (nonBufferBody) {
            return "<non-buffer body>";
        }
        String body = AccessLogBodySupport.formatBody(captured, maxSize);
        if (truncated && body != null && !body.endsWith("...(truncated)")) {
            return body + "...(truncated)";
        }
        return body;
    }
}
