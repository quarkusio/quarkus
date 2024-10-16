package io.quarkus.it.mockbean;

import jakarta.inject.Singleton;

public class SuffixServiceSingletonProducer {

    //@Produces // intentionally commented out to test that auto-producers work with `@InjectMock`
    @Singleton
    public SuffixServiceSingleton dummyService() {
        return new SuffixServiceSingleton();
    }
}
