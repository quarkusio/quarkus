package io.quarkus.hibernate.validator.test;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.validator.test.RepeatedConstraintsTest.MyConstraint.List;
import io.quarkus.test.QuarkusUnitTest;

public class RepeatedConstraintsTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(TestBeanBuiltin.class).addClasses(TestBeanCustom.class));

    @Test
    public void testBuiltinConstraints() {
        assertThat(validatorFactory.getValidator().validate(new TestBeanBuiltin("INVALID"))).hasSize(2);
    }

    @Test
    public void testCustomConstraints() {
        assertThat(validatorFactory.getValidator().validate(new TestBeanCustom("INVALID"))).hasSize(2);
    }

    static class TestBeanBuiltin {

        public TestBeanBuiltin(String testValue) {
            super();
            this.testValue = testValue;
        }

        @Pattern(regexp = ".*[0-9]+.*", message = "Error message")
        @Pattern(regexp = ".*[a-z]+.*", message = "Error message")
        public String testValue;

    }

    static class TestBeanCustom {

        public TestBeanCustom(String testValue) {
            super();
            this.testValue = testValue;
        }

        @MyConstraint("value1")
        @MyConstraint("value2")
        public String testValue;
    }

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Repeatable(List.class)
    @Documented
    @Constraint(validatedBy = { MyConstraintValidator.class })
    public @interface MyConstraint {

        String value();

        String message() default "Error message";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
        @Retention(RUNTIME)
        @Documented
        @interface List {
            MyConstraint[] value();
        }
    }

    @ApplicationScoped
    public static class MyConstraintValidator implements ConstraintValidator<MyConstraint, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return false;
        }
    }
}
