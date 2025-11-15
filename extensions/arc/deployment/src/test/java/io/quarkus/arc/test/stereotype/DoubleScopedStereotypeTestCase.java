package io.quarkus.arc.test.stereotype;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DoubleScopedStereotypeTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DoubleScopedStereotype.class, DoubleScopedStereotypeBean.class))
            .setExpectedException(DefinitionException.class);

    @Inject
    DoubleScopedStereotypeBean bean;

    @Test
    public void runTest() {
        fail();
    }

    @SessionScoped
    @RequestScoped
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DoubleScopedStereotype {
    }

    @DoubleScopedStereotype
    class DoubleScopedStereotypeBean {
    }
}
