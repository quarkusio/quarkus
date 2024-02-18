package io.quarkus.jfr.runtime.http.rest;

public interface Recorder {

    void recordStartEvent();

    void recordEndEvent();

    void startPeriodEvent();

    void endPeriodEvent();
}
