package io.quarkus.arc.test.interceptors.parameters.setter;

import jakarta.enterprise.context.Dependent;

@Dependent
public class TheBean {

    @Setter
    String foo(String val) {
        return val;
    }

}
