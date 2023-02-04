package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConstraintValidatorLocalesTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(MyBean.class)
            .addAsResource("application.properties")
            .addAsResource("ValidationMessages.properties")
            .addAsResource("ValidationMessages_fr_FR.properties"));

    @Test
    public void testConstraintLocale() {
        assertThat(validatorFactory.getValidator().validate(new MyBean("INVALID"))).asString().contains("Non conforme");
    }

    static class MyBean {

        public MyBean(String name) {
            super();
            this.name = name;
        }

        @Pattern(regexp = "A.*", message = "{pattern.message}")
        private String name;
    }
}
