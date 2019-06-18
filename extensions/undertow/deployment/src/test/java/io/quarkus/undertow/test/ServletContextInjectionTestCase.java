/*
 * NOHEADER
 */

package io.quarkus.undertow.test;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ServletContextInjectionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    ServletContext servletContext;

    @Test
    public void testServletContextInjection() {
        Assertions.assertNotNull(servletContext);
    }

}
