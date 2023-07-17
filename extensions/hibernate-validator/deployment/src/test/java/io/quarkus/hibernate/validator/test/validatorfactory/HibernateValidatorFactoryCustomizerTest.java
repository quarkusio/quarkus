package io.quarkus.hibernate.validator.test.validatorfactory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HibernateValidatorFactoryCustomizerTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(MyMultipleHibernateValidatorFactoryCustomizer.class, MyEmailValidator.class,
                    MyNumValidator.class));

    @Test
    public void testOverrideConstraintValidatorConstraint() {
        assertThat(validatorFactory.getValidator().validate(new TestBean())).hasSize(2);
    }

    static class TestBean {
        @Email
        public String email;

        @Min(-1)
        public int num;

        public TestBean() {
            this.email = "test@acme.com";
            this.num = -1;
        }
    }
}
