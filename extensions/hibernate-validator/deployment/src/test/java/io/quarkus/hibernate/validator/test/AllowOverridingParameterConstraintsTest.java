package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AllowOverridingParameterConstraintsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(RealizationWithMethodParameterConstraint.class, InterfaceWithNoConstraints.class,
                    InterfaceWithNotNullMethodParameterConstraint.class,
                    RealizationWithAdditionalMethodParameterConstraint.class)
            .add(new StringAsset(
                    "quarkus.hibernate-validator.method-validation.allow-overriding-parameter-constraints=true"),
                    "application.properties"));

    @Inject
    Validator validator;

    @Test
    public void allowParameterConstraintsAddedInSubType() {
        Set<? extends ConstraintViolation<?>> violations = validator.forExecutables().validateParameters(
                new RealizationWithMethodParameterConstraint(),
                RealizationWithMethodParameterConstraint.class.getDeclaredMethods()[0],
                new Object[] { "foo" });

        assertThat(violations).isEmpty();
    }

    @Test
    public void allowStrengtheningInSubType() {
        Set<ConstraintViolation<RealizationWithAdditionalMethodParameterConstraint>> violations = validator.forExecutables()
                .validateParameters(
                        new RealizationWithAdditionalMethodParameterConstraint(),
                        RealizationWithAdditionalMethodParameterConstraint.class.getDeclaredMethods()[0],
                        new Object[] { "foo" });

        assertThat(violations).isEmpty();
    }

    private interface InterfaceWithNoConstraints {
        String foo(String s);
    }

    private static class RealizationWithMethodParameterConstraint implements InterfaceWithNoConstraints {
        /**
         * Adds constraints to an un-constrained method from a super-type, which is not allowed.
         */
        @Override
        public String foo(@NotNull String s) {
            return "Hello World";
        }
    }

    private interface InterfaceWithNotNullMethodParameterConstraint {
        void bar(@NotNull String s);
    }

    private static class RealizationWithAdditionalMethodParameterConstraint
            implements InterfaceWithNotNullMethodParameterConstraint {
        /**
         * Adds constraints to a constrained method from a super-type, which is not allowed.
         */
        @Override
        public void bar(@Size(min = 3) String s) {
        }
    }
}
