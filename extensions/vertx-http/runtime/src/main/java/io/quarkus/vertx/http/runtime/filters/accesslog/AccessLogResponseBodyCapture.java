package io.quarkus.vertx.http.runtime.filters.accesslog;

import io.vertx.core.buffer.Buffer;

public final class AccessLogResponseBodyCapture {

    private final int maxSize;
    private final Buffer captured = Buffer.buffer();
    private boolean nonBufferBody;

    public AccessLogResponseBodyCapture(int maxSize) {
        this.maxSize = maxSize;
    }

    public void capture(Buffer data) {
        if (nonBufferBody || data == null || data.length() == 0 || captured.length() >= maxSize) {
            return;
        }
        int remaining = maxSize - captured.length();
        if (data.length() <= remaining) {
            captured.appendBuffer(data);
        } else {
            captured.appendBuffer(data.getBuffer(0, remaining));
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
        return AccessLogBodySupport.formatBody(captured, maxSize);
    }
}
