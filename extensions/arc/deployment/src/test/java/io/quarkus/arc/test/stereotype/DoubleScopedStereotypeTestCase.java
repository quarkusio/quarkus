package io.quarkus.arc.test.stereotype;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DoubleScopedStereotypeTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DoubleScopedStereotype.class, DoubleScopedStereotypeBean.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    DoubleScopedStereotypeBean bean;

    @Test
    public void runTest() {
        fail();
    }

    @SessionScoped
    @RequestScoped
    @Stereotype
    public @interface DoubleScopedStereotype {
    }

    @DoubleScopedStereotype
    class DoubleScopedStereotypeBean {
    }
}
