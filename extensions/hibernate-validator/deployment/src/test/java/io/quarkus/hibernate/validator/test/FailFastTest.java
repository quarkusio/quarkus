package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FailFastTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(A.class, B.class)
            .add(new StringAsset("quarkus.hibernate-validator.fail-fast=true"),
                    "application.properties"));

    @Inject
    Validator validator;

    @Test
    public void testFailFastSetOnConfiguration() {
        Set<ConstraintViolation<A>> constraintViolations = validator.validate(new A());

        assertThat(constraintViolations).hasSize(1);
    }

    class A {
        @NotNull
        String b;

        @NotNull
        @Email
        String c;

        @Pattern(regexp = ".*\\.txt$")
        String file;

        @Valid
        Set<B> bs = new HashSet<B>();
    }

    class B {
        @Min(value = 10)
        @Max(value = 30)
        @NotNull
        Integer size;
    }
}
