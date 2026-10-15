package io.quarkus.arc.test.clientproxy.proxynamecollision.alpha;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.test.clientproxy.proxynamecollision.MyBean;
import io.quarkus.arc.test.clientproxy.proxynamecollision.Source;

@Singleton
public class BeanProducer {

    @Produces
    @ApplicationScoped
    @Source("alpha")
    MyBean myBean() {
        return new MyBean("alpha");
    }
}
