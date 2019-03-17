package io.quarkus.arc.app.classes.tests;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

@ApplicationScoped
@Alternative
@Priority(10)
public class SomeOtherService extends SomeService {

    @Override
    public String execute() {
        return "someOther";
    }
}
