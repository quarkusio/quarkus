package io.quarkus.arc.test.decorators.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleDecoratorsTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Converter.class, ToUpperCaseConverter.class,
            AlphaConverterDecorator.class, BravoConverterDecorator.class);

    @Test
    public void testDecoration() {
        ToUpperCaseConverter converter = Arc.container().instance(ToUpperCaseConverter.class).get();
        assertEquals("JE", converter.convert("hej"));
    }

    @ApplicationScoped
    static class ToUpperCaseConverter implements Converter<String> {

        @Override
        public String convert(String value) {
            return value.toUpperCase();
        }

    }

}
