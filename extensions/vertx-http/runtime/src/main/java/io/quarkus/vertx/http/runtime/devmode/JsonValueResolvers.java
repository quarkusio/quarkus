package io.quarkus.vertx.http.runtime.devmode;

import javax.enterprise.event.Observes;

import io.quarkus.qute.EngineBuilder;

public class JsonValueResolvers {

    static void register(@Observes EngineBuilder builder) {
        builder.addValueResolver(new JsonObjectValueResolver());
    }

}
