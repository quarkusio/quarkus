package io.quarkus.it.mockbean;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class SuffixServiceSingletonProducer {

    @Produces
    @Singleton
    public SuffixServiceSingleton dummyService() {
        return new SuffixServiceSingleton();
    }
}
