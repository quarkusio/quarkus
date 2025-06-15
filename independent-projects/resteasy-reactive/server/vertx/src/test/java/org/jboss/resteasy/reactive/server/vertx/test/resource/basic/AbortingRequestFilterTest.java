package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ClassLevelMediaTypeResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class AbortingRequestFilterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(ClassLevelMediaTypeResource.class, AbortingRequestFilter.class);
            return war;
        }
    });

    @Test
    public void testAbortingRequestFilter() {
        RestAssured.get("/test").then().body(Matchers.equalTo("aborted")).statusCode(555);
    }
}
