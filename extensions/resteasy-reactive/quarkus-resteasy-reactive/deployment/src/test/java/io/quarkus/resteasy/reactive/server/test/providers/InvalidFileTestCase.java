package io.quarkus.resteasy.reactive.server.test.providers;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidFileTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidFileResource.class, WithWriterInterceptor.class, WriterInterceptor.class))
            .assertException(t -> {
                while (t.getCause() != null)
                    t = t.getCause();
                Assertions.assertTrue(
                        t.getMessage().equals("Endpoints that return an AsyncFile cannot have any WriterInterceptor set"));
            });

    @Test
    public void test() throws Exception {
        Assertions.fail("Deployment should have failed");
    }
}
