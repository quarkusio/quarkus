package io.quarkus.resteasy.reactive.server.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Level;

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
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

public class VirtualThreadsConfigWithRunOnVirtualThreadApplicationTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.rest.blocking.signatures.execution.mode", "virtual-thread")
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)
                    && record.getLoggerName().equals("io.quarkus.resteasy.reactive.server"))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LOG_FORMATTER::formatMessage)
                    .containsExactly(
                            "The 'quarkus.rest.blocking.signatures.execution.mode' property is ignored because an annotation"
                                    + " on the 'Application' class already set the execution model to 'RUN_ON_VIRTUAL_THREAD'"))
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(VirtualThreadApplication.class, ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/tname/default")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));

        RestAssured.get("/tname/blocking")
                .then().body(Matchers.containsString("executor"), Matchers.not(Matchers.containsString("virtual")));

        // the annotation semantics apply, so non-blocking signatures also run on a virtual thread
        RestAssured.get("/tname/uni")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("eventloop")));
    }

    @RunOnVirtualThread
    public static class VirtualThreadApplication extends Application {

    }

    @Path("tname")
    public static class ThreadNameResource {

        @Path("default")
        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }

        @Blocking
        @Path("blocking")
        @GET
        public String blocking() {
            return Thread.currentThread().getName();
        }

        @Path("uni")
        @GET
        public Uni<String> uni() {
            return Uni.createFrom().item(Thread.currentThread().getName());
        }
    }

}
