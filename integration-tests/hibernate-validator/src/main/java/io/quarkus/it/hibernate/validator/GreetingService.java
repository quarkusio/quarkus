package io.quarkus.it.hibernate.validator;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;

@ApplicationScoped
@Priority(2)
public class GreetingService {

    public String greeting(@NotNull String name) {
        return "hello " + name;
    }

}
