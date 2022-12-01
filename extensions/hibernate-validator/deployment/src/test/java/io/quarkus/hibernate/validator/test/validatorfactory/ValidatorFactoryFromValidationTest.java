package io.quarkus.hibernate.validator.test.validatorfactory;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ValidatorFactoryFromValidationTest {

    @Inject
    ValidatorFactory validatorFactoryFromInjection;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class));

    @Test
    public void testOverrideConstraintValidatorConstraint() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        assertThat(validatorFactoryFromInjection).isSameAs(validatorFactory);
    }

}
