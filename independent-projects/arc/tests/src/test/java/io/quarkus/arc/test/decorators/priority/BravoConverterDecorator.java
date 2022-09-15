package io.quarkus.arc.test.decorators.priority;

import io.quarkus.arc.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

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
