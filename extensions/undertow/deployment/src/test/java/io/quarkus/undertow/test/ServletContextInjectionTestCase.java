package io.quarkus.undertow.test;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ServletContextInjectionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    ServletContext servletContext;

    @Test
    public void testServletContextInjection() {
        Assertions.assertNotNull(servletContext);
        Assertions.assertEquals(4, servletContext.getMajorVersion());
    }

}
