package io.quarkus.tika.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.tika.TikaParser;

@ApplicationScoped
public class TikaParserProducer {

    private volatile TikaParser parser;

    void initialize(TikaParser parser) {
        this.parser = parser;
    }

    @Singleton
    @Produces
    public TikaParser tikaParser() {
        return parser;
    }
}
