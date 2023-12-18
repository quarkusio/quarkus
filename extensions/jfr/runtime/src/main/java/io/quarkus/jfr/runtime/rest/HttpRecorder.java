package io.quarkus.jfr.runtime.rest;

public interface HttpRecorder {

    void recordRequest();

    void recordResponse();
}
