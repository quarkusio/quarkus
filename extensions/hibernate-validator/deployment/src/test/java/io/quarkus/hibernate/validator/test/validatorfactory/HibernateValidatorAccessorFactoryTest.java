package io.quarkus.hibernate.validator.test.validatorfactory;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.HibernateValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateValidatorAccessorFactoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ValidatedBean.class));

    @Inject
    HibernateValidatorFactory validatorFactory;

    @Test
    void usesGeneratedAccessorsForValidation() {
        assertThat(validatorFactory.getAccessorFactory().getClass().getName())
                .isEqualTo("io.quarkus.hibernate.accessor.runtime.QuarkusHibernateAccessorFactory");
        assertThat(Validation.buildDefaultValidatorFactory().unwrap(HibernateValidatorFactory.class).getAccessorFactory())
                .isSameAs(validatorFactory.getAccessorFactory());

        assertThat(validatorFactory.getValidator().validate(new ValidatedBean(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("value", "getterValue");
    }

    static class ValidatedBean {
        @NotNull
        private final String value;

        ValidatedBean(String value) {
            this.value = value;
        }

        @NotNull
        String getGetterValue() {
            return value;
        }
    }
}
