package io.quarkus.arc.test.interceptors.complex;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

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
