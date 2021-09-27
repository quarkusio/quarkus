package io.quarkus.test.vertx;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class UuidMessageConsumer {

    @ConsumeEvent(value = "event", blocking = true)
    public String handleUuid(UUID uuid) {
        return "test-" + uuid.toString();
    }
}
