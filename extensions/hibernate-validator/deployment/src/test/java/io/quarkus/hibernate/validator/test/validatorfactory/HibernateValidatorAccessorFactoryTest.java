package io.quarkus.hibernate.validator.test.validatorfactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

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

    @Test
    void validationDoesNotRequestWritersOrRemoveFinal() throws Exception {
        var field = ValidatedBean.class.getDeclaredField("value");
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        assertThatThrownBy(() -> validatorFactory.getAccessorFactory().valueWriter(field))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    static class ValidatedBean {
        @NotNull
        static String staticValue;

        static List<@NotNull String> staticValues = Collections.singletonList(null);

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
