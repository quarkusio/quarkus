package io.quarkus.resteasy.reactive.server.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.spi.TargetJavaVersionBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

@EnabledForJreRange(min = JRE.JAVA_19)
public class ApplicationWithRunOnVirtualThreadNonBlockingTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            // we need this to make sure that the build doesn't fail because of the target bytecode version being JDK 17
            .addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder.addBuildStep(context -> {
                context.produce(new TargetJavaVersionBuildItem(new DummyTargetJavaVersion()));
            }).produces(TargetJavaVersionBuildItem.class).build())
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.SEVERE)
                    && record.getLoggerName()
                            .equals("org.jboss.resteasy.reactive.server.core.startup.RuntimeResourceDeployment"))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                    .isNotEmpty()
                    .allMatch(msg -> msg.startsWith(
                            "a method was both @NonBlocking and @RunOnVirtualThread, it is now considered @RunOnVirtualThread and @Blocking")))
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(VirtualThreadApplication.class, ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/tname/nonblocking")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));
    }

    @RunOnVirtualThread
    public static class VirtualThreadApplication extends Application {

    }

    @Path("tname")
    public static class ThreadNameResource {

        @Path("nonblocking")
        @GET
        @NonBlocking
        public String threadName() {
            return Thread.currentThread().getName();
        }
    }

    public static class DummyTargetJavaVersion implements TargetJavaVersion {

        @Override
        public Status isJava19OrHigher() {
            return Status.TRUE;
        }
    }

}
