package io.quarkus.resteasy.reactive.server.test.duplicate;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DuplicateResourceDetectionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource.class, GreetingResource2.class, GreetingResource3.class))
            .assertException(throwable -> Assertions.assertThat(throwable)
                    .hasMessage("GET /hello-resteasy is declared by :" + System.lineSeparator()
                            + "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource#helloGet consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator()
                            + "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource#helloGetNoExplicitMimeType consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator()
                            + "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource2#helloGet consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator()));

    @Test
    public void dummy() {

    }
}
