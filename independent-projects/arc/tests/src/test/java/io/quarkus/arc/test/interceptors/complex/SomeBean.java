package io.quarkus.arc.test.interceptors.complex;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@MyBinding
public class SomeBean {

    public static AtomicBoolean preDestroyInvoked = new AtomicBoolean(false);
    public static AtomicBoolean postConstructInvoked = new AtomicBoolean(false);

    @PreDestroy
    public void preDestroy() {
        preDestroyInvoked.set(true);
    }

    @PostConstruct
    public void postConstruct() {
        postConstructInvoked.set(true);
    }

    public void ping() {

    }

    public static void reset() {
        preDestroyInvoked.set(false);
        postConstructInvoked.set(false);
    }
}
