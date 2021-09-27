package io.quarkus.arc.test.decorators.priority;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

@Priority(2)
@Decorator
class BravoConverterDecorator implements Converter<String> {

    Converter<String> delegate;

    @Inject
    public BravoConverterDecorator(@Delegate Converter<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String convert(String value) {
        return new StringBuilder(delegate.convert(value)).reverse().toString();
    }

}
