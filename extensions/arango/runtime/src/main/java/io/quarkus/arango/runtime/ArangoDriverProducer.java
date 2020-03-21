package io.quarkus.arango.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.arangodb.ArangoDB;

@ApplicationScoped
public class ArangoDriverProducer {

    private ArangoDB driver;

    void initialize(ArangoDB driver) {
        this.driver = driver;
    }

    @Singleton
    @Produces
    public ArangoDB driver() {
        return driver;
    }
}
