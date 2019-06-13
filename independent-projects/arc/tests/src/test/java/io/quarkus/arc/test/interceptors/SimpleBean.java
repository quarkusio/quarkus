package io.quarkus.arc.test.interceptors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Lifecycle
@Dependent
public class SimpleBean {

    private String val;

    private Counter counter;

    @Inject
    SimpleBean(Counter counter) {
        this.counter = counter;
    }

    @PostConstruct
    void superCoolInit() {
        val = "foo";
    }

    @Logging
    @Simple
    String foo(String anotherVal) {
        return val;
    }

    String bar() {
        return new StringBuilder(val).reverse().toString();
    }

    @Simple
    void baz(Integer dummy) {
    }

    Counter getCounter() {
        return counter;
    }

}
