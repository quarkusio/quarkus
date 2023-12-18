package io.quarkus.jfr.runtime;

public interface RequestIdProducer {

    <T extends RequestId> T create();
}
