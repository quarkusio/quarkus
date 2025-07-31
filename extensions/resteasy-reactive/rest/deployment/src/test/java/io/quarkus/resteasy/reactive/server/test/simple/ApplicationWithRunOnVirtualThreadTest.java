package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.function.Supplier;

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
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

@EnabledForJreRange(min = JRE.JAVA_19)
public class ApplicationWithRunOnVirtualThreadTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            // we need this to make sure that the build doesn't fail because of the target bytecode version being JDK 17
            .addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder.addBuildStep(context -> {
                context.produce(new TargetJavaVersionBuildItem(new DummyTargetJavaVersion()));
            }).produces(TargetJavaVersionBuildItem.class).build())
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

        RestAssured.get("/tname/virtual")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));
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

        @RunOnVirtualThread
        @Path("virtual")
        @GET
        public String virtual() {
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
