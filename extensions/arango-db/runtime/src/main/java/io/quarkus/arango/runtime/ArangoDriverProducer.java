package io.quarkus.arango.runtime;

import com.arangodb.ArangoDB;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@ApplicationScoped
public class ArangoDriverProducer {

    private volatile ArangoDB driver;

    void initialize(ArangoDB driver) {
        this.driver = driver;
    }

    @Singleton
    @Produces
    public ArangoDB driver() {
        return driver;
    }
}
