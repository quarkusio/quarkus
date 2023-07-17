package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

public class ApplicationWithBlockingTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(BlockingApplication.class, ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/tname/blocking")
                .then().body(Matchers.containsString("executor"), Matchers.not(Matchers.containsString("loop")));

        RestAssured.get("/tname/nonblocking")
                .then().body(Matchers.containsString("loop"), Matchers.not(Matchers.containsString("executor")));

        RestAssured.get("/tname2/blocking")
                .then().body(Matchers.containsString("executor"), Matchers.not(Matchers.containsString("loop")));

        RestAssured.get("/tname2/nonblocking")
                .then().body(Matchers.containsString("loop"), Matchers.not(Matchers.containsString("executor")));
    }

    @Blocking
    public static class BlockingApplication extends Application {

    }

    @Path("tname")
    public static class ThreadNameResource {

        @Path("blocking")
        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }

        @NonBlocking
        @Path("nonblocking")
        @GET
        public String nonBlocking() {
            return Thread.currentThread().getName();
        }
    }

    @Blocking // this should have no effect
    @Path("tname2")
    public static class ThreadNameResourceWithBlocking {

        @Path("blocking")
        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }

        @NonBlocking
        @Path("nonblocking")
        @GET
        public String nonBlocking() {
            return Thread.currentThread().getName();
        }
    }

}
