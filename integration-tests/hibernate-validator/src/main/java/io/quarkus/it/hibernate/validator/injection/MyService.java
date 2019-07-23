package io.quarkus.it.hibernate.validator.injection;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyService {

    public static final String VALID = "VALID";

    public boolean validate(String value) {
        return VALID.equals(value);
    }
}
