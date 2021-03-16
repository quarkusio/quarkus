package io.quarkus.reactivemessaging.http.runtime.config;

public class WebSocketStreamConfig extends StreamConfigBase {
    public final String path;

    public WebSocketStreamConfig(String path, int bufferSize) {
        super(bufferSize);
        this.path = path;
    }

    public String path() {
        return path;
    }
}
