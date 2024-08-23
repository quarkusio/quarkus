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

public class DoubleScopedStereotypeHierarchyTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SessionStereotype.class, RequestStereotype.class, AnotherStereotype.class,
                            DoubleScopedStereotypeBean.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    DoubleScopedStereotypeBean bean;

    @Test
    public void runTest() {
        fail();
    }

    @SessionScoped
    @Stereotype
    public @interface SessionStereotype {
    }

    @RequestScoped
    @Stereotype
    public @interface RequestStereotype {
    }

    @RequestStereotype
    @SessionStereotype
    @Stereotype
    public @interface AnotherStereotype {
    }

    @AnotherStereotype
    class DoubleScopedStereotypeBean {
    }
}
