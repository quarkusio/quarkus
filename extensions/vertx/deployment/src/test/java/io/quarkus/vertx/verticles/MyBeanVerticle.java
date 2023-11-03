package io.quarkus.vertx.verticles;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

@ApplicationScoped
public class MyBeanVerticle extends AbstractVerticle {

    @ConfigProperty(name = "address")
    String address;

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer(address)
                .handler(m -> m.reply("hello"))
                .completionHandler();
    }
}
