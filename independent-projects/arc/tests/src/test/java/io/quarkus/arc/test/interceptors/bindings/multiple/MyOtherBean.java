package io.quarkus.arc.test.interceptors.bindings.multiple;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@FooBinding
@BarBinding
public class MyOtherBean {

    public String foo() {
        return "anotherFoo";
    }
}
