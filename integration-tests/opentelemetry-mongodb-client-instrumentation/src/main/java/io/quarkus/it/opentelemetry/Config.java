package io.quarkus.it.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.mongodb.runtime.MongoClientCustomizer;

@ApplicationScoped
public class Config {
    @Produces
    public MongoClientCustomizer mongoClientCustomizer() {
        return builder -> builder.applicationName("opentelemetry-mongodb-integration-test");
    }

}
