package io.quarkus.arc.test.supplement.decorator;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

@Decorator
@Priority(10)
public class SomeDecorator implements SomeInterface {

    @Inject
    @Delegate
    SomeInterface delegate;

    @Override
    public String ping() {
        return "Delegated: " + delegate.ping();
    }

}
