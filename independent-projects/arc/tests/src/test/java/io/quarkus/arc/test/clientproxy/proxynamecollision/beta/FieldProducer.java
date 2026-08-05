package io.quarkus.arc.test.clientproxy.proxynamecollision.beta;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.test.clientproxy.proxynamecollision.MyBean;
import io.quarkus.arc.test.clientproxy.proxynamecollision.Source;

@Singleton
public class FieldProducer {

    @Produces
    @RequestScoped
    @Source("betaField")
    MyBean myBean = new MyBean("betaField");
}
