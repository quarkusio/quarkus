package io.quarkus.reactivemessaging.utils;

import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.vertx.core.buffer.Buffer;

public class ToUpperCaseSerializer implements Serializer<String> {
    private static final Logger log = Logger.getLogger(ToUpperCaseSerializer.class);

    @Override
    public boolean handles(Object payload) {
        log.infof("checking if %s of type %s is handled", payload, payload.getClass());
        return payload instanceof String;
    }

    @Override
    public Buffer serialize(String payload) {
        log.infof("serializing %s", payload);
        return Buffer.buffer(payload.toUpperCase());
    }
}
