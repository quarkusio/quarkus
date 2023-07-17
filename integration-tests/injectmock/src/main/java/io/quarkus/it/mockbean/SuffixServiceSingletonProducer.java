package io.quarkus.it.mockbean;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

public class SuffixServiceSingletonProducer {

    @Produces
    @Singleton
    public SuffixServiceSingleton dummyService() {
        return new SuffixServiceSingleton();
    }
}
