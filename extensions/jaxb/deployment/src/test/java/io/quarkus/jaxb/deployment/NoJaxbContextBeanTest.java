package io.quarkus.jaxb.deployment;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Make sure that the default JAXB context is not validated at build time as long as there is no injection point for it.
 * Conflicting model classes thus won't make the application fail.
 */
public class NoJaxbContextBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(io.quarkus.jaxb.deployment.one.Model.class, io.quarkus.jaxb.deployment.two.Model.class));

    @Test
    @ActivateRequestContext
    public void noJaxbContext() {
        InstanceHandle<JAXBContext> contextHandle = Arc.container().instance(JAXBContext.class);
        Assertions.assertFalse(contextHandle.isAvailable());
    }

}
