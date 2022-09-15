package io.quarkus.it.hibernate.validator;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;

@ApplicationScoped
@Priority(2)
public class GreetingService {

    public String greeting(@NotNull String name) {
        return "hello " + name;
    }

}
