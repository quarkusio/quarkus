package io.quarkus.arc.test.interceptors.bindings.multiple;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@BarBinding
public class MyBean {

    public String foo() {
        return "foo";
    }
}
