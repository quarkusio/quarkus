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

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;

public class VirtualThreadsConfigWithApplicationAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.rest.virtual-threads", "true")
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NonBlockingApplication.class, ThreadNameResource.class);
                }
            });

    @Test
    public void test() {
        // the annotation on the Application class wins over 'quarkus.rest.virtual-threads'
        RestAssured.get("/tname/default")
                .then().body(Matchers.containsString("eventloop"), Matchers.not(Matchers.containsString("virtual")));
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
    }

}
