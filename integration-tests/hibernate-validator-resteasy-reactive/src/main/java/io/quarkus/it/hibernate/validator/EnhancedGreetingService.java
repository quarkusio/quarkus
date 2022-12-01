package io.quarkus.it.hibernate.validator;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@ApplicationScoped
public class EnhancedGreetingService extends GreetingService {

    @Override
    public String greeting(String name) {
        return super.greeting("Enhanced " + name);
    }
}
