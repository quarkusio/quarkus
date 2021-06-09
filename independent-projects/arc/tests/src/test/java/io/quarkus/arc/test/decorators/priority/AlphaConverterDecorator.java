package io.quarkus.arc.test.decorators.priority;

import io.quarkus.arc.test.decorators.priority.MultipleDecoratorsTest.Converter;
import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

@Priority(20)
@Decorator
class AlphaConverterDecorator implements Converter<String> {

    @Inject
    @Delegate
    Converter<String> delegate;

    @Override
    public String convert(String value) {
        // skip first char
        return value.substring(1);
    }

}
