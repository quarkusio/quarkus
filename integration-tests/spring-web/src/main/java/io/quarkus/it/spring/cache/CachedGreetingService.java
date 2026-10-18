package io.quarkus.it.spring.cache;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CachedGreetingService {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Cacheable("someCache")
    public Greeting greet(String name) {
        return new Greeting("Hello " + name, counter.getAndIncrement());
    }
}
