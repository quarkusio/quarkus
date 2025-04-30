package io.quarkus.jfr.runtime;

public interface IdProducer {

    String getTraceId();

    String getSpanId();
}
