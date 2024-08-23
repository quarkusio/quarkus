package io.quarkus.hibernate.validator.test;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AllowMultipleCascadedValidationOnReturnValuesTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(SubRealizationWithValidConstraintOnMethodParameter.class,
                    RealizationWithValidConstraintOnMethodParameter.class,
                    InterfaceWithNoConstraints.class)
            .add(new StringAsset(
                    "quarkus.hibernate-validator.method-validation.allow-multiple-cascaded-validation-on-return-values=true"),
                    "application.properties"));

    @Inject
    Validator validator;

    @Test
    public void allowValidAddedInSubType() {
        validator.forExecutables().validateParameters(
                new SubRealizationWithValidConstraintOnMethodParameter(),
                SubRealizationWithValidConstraintOnMethodParameter.class.getDeclaredMethods()[0],
                new Object[] { "foo" });
    }

    private interface InterfaceWithNoConstraints {
        String foo(String s);
    }

    private static class RealizationWithValidConstraintOnMethodParameter
            implements InterfaceWithNoConstraints {
        /**
         * Adds @Valid to an un-constrained method from a super-type, which is not allowed.
         */
        @Override
        @Valid
        public String foo(String s) {
            return "Hello Valid World";
        }
    }

    private static class SubRealizationWithValidConstraintOnMethodParameter
            extends RealizationWithValidConstraintOnMethodParameter {
        /**
         * Adds @Valid to an un-constrained method from a super-type, which is not allowed.
         */
        @Override
        @Valid
        public String foo(String s) {
            return "Hello Valid World";
        }
    }

}
