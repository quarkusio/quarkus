package io.quarkus.resteasy.reactive.server.test.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DuplicateResourceWarningTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource.class, GreetingResource2.class, GreetingResource3.class))
            .overrideConfigKey("quarkus.rest.fail-on-duplicate", "false")
            .setLogRecordPredicate(item -> item.getLevel().equals(Level.WARNING)
                    && item.getLoggerName().equals("io.quarkus.resteasy.reactive.server"))
            .assertLogRecords(logs -> assertThat(logs.stream().map(LogRecord::getMessage).collect(Collectors.toList()))
                    .contains("GET /hello-resteasy is declared by :" + System.lineSeparator() +
                            "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource#helloGet consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator() +
                            "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource#helloGetNoExplicitMimeType consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator() +
                            "io.quarkus.resteasy.reactive.server.test.duplicate.GreetingResource2#helloGet consumes *, produces text/plain;charset=UTF-8"
                            + System.lineSeparator()));

    @Test
    public void dummy() {

    }
}
