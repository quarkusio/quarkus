package io.quarkus.hibernate.validator.test;

import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AllowParameterConstraintsOnParallelMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(RealizationOfTwoInterface.class, InterfaceWithNoConstraints.class,
                    AnotherInterfaceWithMethodParameterConstraint.class)
            .add(new StringAsset(
                    "quarkus.hibernate-validator.method-validation.allow-parameter-constraints-on-parallel-methods=true"),
                    "application.properties"));

    @Inject
    Validator validator;

    @Test
    public void allowParameterConstraintsInHierarchyWithMultipleRootMethods() {
        validator.forExecutables().validateParameters(
                new RealizationOfTwoInterface(),
                RealizationOfTwoInterface.class.getDeclaredMethods()[0],
                new Object[] { "foo" });
    }

    private interface InterfaceWithNoConstraints {
        String foo(String s);
    }

    private interface AnotherInterfaceWithMethodParameterConstraint {
        String foo(@NotNull String s);
    }

    private static class RealizationOfTwoInterface
            implements InterfaceWithNoConstraints, AnotherInterfaceWithMethodParameterConstraint {
        /**
         * Implement a method that is declared by two interfaces, one of which has a constraint
         */
        @Override
        public String foo(String s) {
            return "Hello World";
        }
    }
}
