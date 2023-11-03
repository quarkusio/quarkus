package io.quarkus.hibernate.validator.test;

import jakarta.inject.Inject;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConstraintOnStaticMethodTest {

    @Inject
    ValidatorFactory validatorFactory;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(MyUtil.class));

    @Test
    public void testStaticMethodIsIgnored() {
        MyUtil.validateName("My name");
    }

    public static class MyUtil {

        public static void validateName(@Pattern(regexp = "A.*") String name) {
            // do nothing
        }
    }
}
