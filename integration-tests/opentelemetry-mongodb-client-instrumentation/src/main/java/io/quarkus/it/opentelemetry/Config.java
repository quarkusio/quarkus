package io.quarkus.it.opentelemetry;

import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class Config {
    @Produces
    public MongoClientCustomizer mongoClientCustomizer() {
        return builder -> builder.applicationName("opentelemetry-mongodb-integration-test");
    }

}
