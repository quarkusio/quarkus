package io.quarkus.arc.test.decorators.priority;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

@Priority(20)
@Decorator
class AlsoAlphaConverterDecorator implements Converter<String> {

    @Inject
    @Delegate
    Converter<String> delegate;

    @Override
    public String convert(String value) {
        // skip first char
        return delegate.convert(value).substring(1);
    }

}
