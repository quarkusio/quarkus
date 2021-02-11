package io.quarkus.arc.test.stereotype;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DoubleScopedBeanTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SessionStereotype.class, RequestStereotype.class, DoubleScopedStereotypeBean.class))
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

    @SessionStereotype
    @RequestStereotype
    class DoubleScopedStereotypeBean {
    }
}
