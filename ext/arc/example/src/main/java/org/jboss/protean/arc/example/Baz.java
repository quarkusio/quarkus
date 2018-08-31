package org.jboss.protean.arc.example;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@ApplicationScoped
public class Baz {

    @MyQualifier(alpha = "1", bravo = "1")
    @Inject
    Foo foo;

    @Inject
    void setBar(Bar bar) {
    }

    public String pingFoo() {
        return foo.ping();
    }

    @MyQualifier(bravo = "1")
    @Produces
    public List<String> listProducer(InjectionPoint injectionPoint) {
        return Arrays.asList(injectionPoint.getType().toString());
    }

}
