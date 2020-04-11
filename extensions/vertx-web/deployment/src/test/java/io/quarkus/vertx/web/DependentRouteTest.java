package io.quarkus.vertx.web;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.RoutingContext;

public class DependentRouteTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleBean.class));

    @Test
    public void testSimpleRoute() {
        assertFalse(SimpleBean.DESTROYED.get());
        when().get("/hello").then().statusCode(200).body(is("Hello!"));
        assertTrue(SimpleBean.DESTROYED.get());
    }

    @Dependent
    static class SimpleBean {

        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Route(path = "/hello")
        void hello(RoutingContext context) {
            context.response().setStatusCode(200).end("Hello!");
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

}
