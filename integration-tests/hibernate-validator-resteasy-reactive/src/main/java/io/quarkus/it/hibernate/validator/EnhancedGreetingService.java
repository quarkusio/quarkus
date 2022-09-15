package io.quarkus.it.hibernate.validator;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@ApplicationScoped
public class EnhancedGreetingService extends GreetingService {

    @Override
    public String greeting(String name) {
        return super.greeting("Enhanced " + name);
    }
}
