package io.quarkus.arc.test.transform.injectionPoint;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

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
