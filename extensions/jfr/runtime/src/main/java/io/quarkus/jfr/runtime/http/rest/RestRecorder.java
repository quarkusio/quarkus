package io.quarkus.jfr.runtime.http.rest;

public interface RestRecorder {

    void recordRequest();

    void recordResponse();
}
