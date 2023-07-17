package io.quarkus.hibernate.validator.test;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validator;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConstraintExpressionLanguageFeatureLevelTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(BeanMethodsConstraint.class, BeanMethodsConstraintStringValidator.class, BeanMethodsBean.class)
            .add(new StringAsset(
                    "quarkus.hibernate-validator.expression-language.constraint-expression-feature-level=bean-methods"),
                    "application.properties"));

    @Inject
    Validator validator;

    @Test
    public void testConstraintExpressionFeatureLevel() {
        Set<ConstraintViolation<BeanMethodsBean>> violations = validator.validate(new BeanMethodsBean());
        assertEquals("Method execution: a", violations.iterator().next().getMessage());
    }

    @Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { BeanMethodsConstraintStringValidator.class })
    private @interface BeanMethodsConstraint {
        String message() default "Method execution: ${'aaaa'.substring(0, 1)}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class BeanMethodsConstraintStringValidator implements ConstraintValidator<BeanMethodsConstraint, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return false;
        }
    }

    public static class BeanMethodsBean {

        @BeanMethodsConstraint
        public String value;
    }
}
