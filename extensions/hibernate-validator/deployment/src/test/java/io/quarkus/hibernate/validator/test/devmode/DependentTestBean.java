package io.quarkus.hibernate.validator.test.devmode;

import jakarta.enterprise.context.Dependent;

@Dependent
public class DependentTestBean {
    public String testMethod(/* <placeholder> */ String message) {
        return message;
    }
}
