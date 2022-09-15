package io.quarkus.arc.test.decorators.qualifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DelegateQualifiersTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            TrimConverterDecorator.class, MyQualifier.class);

    @Test
    public void testDecoration() {
        @SuppressWarnings("serial")
        ToUpperCaseConverter converter = Arc.container()
                .instance(ToUpperCaseConverter.class, new AnnotationLiteral<MyQualifier>() {
                }).get();
        assertEquals("HOLA!", converter.convert(" holA!"));
        assertEquals(" HOLA!", converter.convertNoDelegation(" holA!"));
    }

    interface Converter<T> {

        T convert(T value);

    }

    @MyQualifier
    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

        public String convertNoDelegation(String value) {
            return value.toUpperCase();
        }

    }

    @Dependent
    @Priority(1)
    @Decorator
    static class TrimConverterDecorator implements Converter<String> {

        @Inject
        @MyQualifier
        @Delegate
        Converter<String> delegate;

        @Override
        public String convert(String value) {
            return delegate.convert(value.trim());
        }

    }

}
