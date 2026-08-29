package io.quarkus.resteasy.reactive.server.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.spi.TargetJavaVersionBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

public class VirtualThreadsConfigTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            // we need this to make sure that the build doesn't fail because of the target bytecode version being JDK 17
            .addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder.addBuildStep(context -> {
                context.produce(new TargetJavaVersionBuildItem(new DummyTargetJavaVersion()));
            }).produces(TargetJavaVersionBuildItem.class).build())
            .overrideConfigKey("quarkus.rest.virtual-threads", "true")
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.SEVERE)
                    && record.getLoggerName()
                            .equals("org.jboss.resteasy.reactive.server.core.startup.RuntimeResourceDeployment"))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty())
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        // blocking signature without an annotation defaults to a virtual thread
        RestAssured.get("/tname/default")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));

        // an explicit @Blocking annotation keeps the endpoint on a worker thread
        RestAssured.get("/tname/blocking")
                .then().body(Matchers.containsString("executor"), Matchers.not(Matchers.containsString("virtual")));

        RestAssured.get("/tname/virtual")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));

        // non-blocking endpoints stay on the event loop
        RestAssured.get("/tname/nonblocking")
                .then().body(Matchers.containsString("eventloop"), Matchers.not(Matchers.containsString("virtual")));

        RestAssured.get("/tname/uni")
                .then().body(Matchers.containsString("eventloop"), Matchers.not(Matchers.containsString("virtual")));
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

        @RunOnVirtualThread
        @Path("virtual")
        @GET
        public String virtual() {
            return Thread.currentThread().getName();
        }

        @NonBlocking
        @Path("nonblocking")
        @GET
        public String nonBlocking() {
            return Thread.currentThread().getName();
        }

        @Path("uni")
        @GET
        public Uni<String> uni() {
            return Uni.createFrom().item(Thread.currentThread().getName());
        }
    }

    public static class DummyTargetJavaVersion implements TargetJavaVersion {

        @Override
        public Status isJava19OrHigher() {
            return Status.TRUE;
        }
    }

}
