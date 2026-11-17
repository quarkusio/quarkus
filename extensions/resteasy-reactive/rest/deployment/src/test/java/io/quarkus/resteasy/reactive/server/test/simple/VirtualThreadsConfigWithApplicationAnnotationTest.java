package io.quarkus.resteasy.reactive.server.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class VirtualThreadsConfigWithApplicationAnnotationTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.rest.blocking.signatures.execution.mode", "virtual-thread")
            .setLogRecordPredicate(record -> (record.getLevel().equals(Level.SEVERE)
                    && record.getLoggerName()
                            .equals("org.jboss.resteasy.reactive.server.core.startup.RuntimeResourceDeployment"))
                    || (record.getLevel().equals(Level.WARNING)
                            && record.getLoggerName().equals("io.quarkus.resteasy.reactive.server")))
            .assertLogRecords(records -> {
                assertThat(records).filteredOn(r -> r.getLevel().equals(Level.WARNING))
                        .extracting(LOG_FORMATTER::formatMessage)
                        .containsExactly(
                                "The 'quarkus.rest.blocking.signatures.execution.mode' property is ignored because an annotation"
                                        + " on the 'Application' class already set the execution model to 'NON_BLOCKING'");
                assertThat(records).filteredOn(r -> r.getLevel().equals(Level.SEVERE))
                        .extracting(LogRecord::getMessage)
                        .isNotEmpty()
                        .allMatch(msg -> msg.startsWith(
                                "a method was both @NonBlocking and @RunOnVirtualThread, it is now considered @RunOnVirtualThread and @Blocking"));
            })
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NonBlockingApplication.class, ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/tname/default")
                .then().body(Matchers.containsString("eventloop"), Matchers.not(Matchers.containsString("virtual")));

        RestAssured.get("/tname/virtual")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));
    }

    @NonBlocking
    public static class NonBlockingApplication extends Application {

    }

    @Path("tname")
    public static class ThreadNameResource {

        @Path("default")
        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }

        @RunOnVirtualThread
        @Path("virtual")
        @GET
        public String virtual() {
            return Thread.currentThread().getName();
        }
    }

}
