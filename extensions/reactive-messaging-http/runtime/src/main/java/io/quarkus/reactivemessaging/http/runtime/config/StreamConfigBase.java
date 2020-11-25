package io.quarkus.reactivemessaging.http.runtime.config;

public class StreamConfigBase {
    public final int bufferSize;

    public StreamConfigBase(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
