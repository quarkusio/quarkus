package io.quarkus.arc.test.clientproxy.proxynamecollision.beta;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.test.clientproxy.proxynamecollision.MyBean;
import io.quarkus.arc.test.clientproxy.proxynamecollision.Source;

@Singleton
public class BeanProducer {

    @Produces
    @RequestScoped
    @Source("beta")
    MyBean myBean() {
        return new MyBean("beta");
    }
}
