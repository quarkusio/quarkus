package io.quarkus.hibernate.validator.test.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;
import io.quarkus.test.QuarkusExtensionTest;

public class XmlConstraintAccessorTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(Car.class)
                    .addAsResource("accessor-guide/validation.xml", "META-INF/validation.xml")
                    .addAsResource("accessor-guide/car-constraints.xml", "META-INF/car-constraints.xml"))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    Validator validator;

    @Test
    void validatesXmlConstraint() {
        assertThat(validator.validate(new Car())).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("manufacturer"));
    }

    public static class Car {
        @GeneratedFieldAccessor
        private String manufacturer;
    }
}
