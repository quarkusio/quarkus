package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class VirtualThreadsConfigClassAnnotationsTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.rest.blocking.signatures.execution.mode", "virtual-thread")
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(BlockingResource.class, NonBlockingResource.class, VirtualThreadResource.class);
                }
            });

    @Test
    public void test() {
        // execution model annotations on the resource class take precedence over the property
        RestAssured.get("/blocking")
                .then().body(Matchers.containsString("executor"), Matchers.not(Matchers.containsString("virtual")));

        RestAssured.get("/nonblocking")
                .then().body(Matchers.containsString("eventloop"), Matchers.not(Matchers.containsString("virtual")));

        RestAssured.get("/virtual")
                .then().body(Matchers.containsString("virtual"), Matchers.not(Matchers.containsString("executor")));
    }

    @Blocking
    @Path("blocking")
    public static class BlockingResource {

        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }
    }

    @NonBlocking
    @Path("nonblocking")
    public static class NonBlockingResource {

        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }
    }

    @RunOnVirtualThread
    @Path("virtual")
    public static class VirtualThreadResource {

        @GET
        public String threadName() {
            return Thread.currentThread().getName();
        }
    }

}
