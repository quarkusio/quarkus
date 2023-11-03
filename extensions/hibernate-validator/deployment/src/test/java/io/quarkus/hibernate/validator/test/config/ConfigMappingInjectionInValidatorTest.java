package io.quarkus.hibernate.validator.test.config;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Inject;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Validator;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

class ConfigMappingInjectionInValidatorTest {
    @RegisterExtension
    private static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Validator validator;

    @Test
    void valid() {
        assertTrue(validator.validate(new Entity()).isEmpty());
    }

    @StaticInitSafe
    @ConfigMapping(prefix = "valid.config")
    public interface ValidConfig {
        @WithDefault("true")
        boolean isValid();
    }

    @Target({ TYPE, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = { ValidEntityValidator.class })
    @Documented
    public @interface ValidEntity {
        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class ValidEntityValidator implements ConstraintValidator<ValidEntity, Entity> {
        @Inject
        ValidConfig validConfig;

        @Override
        public boolean isValid(Entity value, ConstraintValidatorContext context) {
            return validConfig.isValid();
        }
    }

    @ValidEntity
    public static class Entity {

    }
}
