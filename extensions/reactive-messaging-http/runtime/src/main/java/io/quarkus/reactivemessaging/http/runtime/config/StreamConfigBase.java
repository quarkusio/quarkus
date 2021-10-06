package io.quarkus.reactivemessaging.http.runtime.config;

public class StreamConfigBase {
    public final int bufferSize;
    public final String path;

    public StreamConfigBase(int bufferSize, String path) {
        this.path = path;
        this.bufferSize = bufferSize;
    }
}
