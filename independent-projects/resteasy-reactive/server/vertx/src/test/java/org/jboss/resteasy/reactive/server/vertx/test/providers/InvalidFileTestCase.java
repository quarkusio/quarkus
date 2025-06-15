package org.jboss.resteasy.reactive.server.vertx.test.providers;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InvalidFileTestCase {

    @RegisterExtension
    static final ResteasyReactiveUnitTest config = new ResteasyReactiveUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(InvalidFileResource.class, WithWriterInterceptor.class, WriterInterceptor.class))
            .assertException(t -> {
                while (t.getCause() != null)
                    t = t.getCause();
                Assertions.assertTrue(t.getMessage()
                        .equals("Endpoints that return an AsyncFile cannot have any WriterInterceptor set"));
            });

    @Test
    public void test() throws Exception {
        Assertions.fail("Deployment should have failed");
    }
}
