package io.quarkus.arc.test.clientproxy.proxynamecollision.alpha;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.test.clientproxy.proxynamecollision.MyBean;
import io.quarkus.arc.test.clientproxy.proxynamecollision.Source;

@Singleton
public class FieldProducer {

    @Produces
    @ApplicationScoped
    @Source("alphaField")
    MyBean myBean = new MyBean("alphaField");
}
