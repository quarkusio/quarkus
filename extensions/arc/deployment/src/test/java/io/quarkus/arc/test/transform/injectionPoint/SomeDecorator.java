package io.quarkus.arc.test.transform.injectionPoint;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
@Priority(1)
@Decorator
public class SomeDecorator implements PrivateFieldInjectionTest.DecoratedBean {

    @Inject
    @Delegate
    PrivateFieldInjectionTest.DecoratedBean delegate;

    @Inject
    private DummyBean bean;

    @Override
    public String ping() {
        return bean.generateString() + delegate.ping();
    }
}
