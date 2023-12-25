package io.quarkus.jfr.runtime.http.rest;

public interface RestRecorder {

    void recordReactiveRequest();

    void recordBlockingRequest();

    void recordReactiveResponse();

    void recordBlockingResponse();
}
