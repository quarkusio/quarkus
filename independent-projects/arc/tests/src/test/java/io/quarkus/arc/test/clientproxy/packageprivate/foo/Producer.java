package io.quarkus.arc.test.clientproxy.packageprivate.foo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class Producer {

    @Produces
    @ApplicationScoped
    public MyInterface2 myInterface2() {
        return new MyInterface2() {
            @Override
            public String ping() {
                return "quarkus";
            }
        };
    }

}
