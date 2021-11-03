package io.quarkus.reactivemessaging.http.runtime.config;

public class WebSocketStreamConfig extends StreamConfigBase {
    public WebSocketStreamConfig(String path, int bufferSize) {
        super(bufferSize, path);
    }

    public String path() {
        return path;
    }
}
