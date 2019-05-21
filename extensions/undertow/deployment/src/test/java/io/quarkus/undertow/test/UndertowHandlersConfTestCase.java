package io.quarkus.undertow.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UndertowHandlersConfTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RedirectedServlet.class)
                    .addAsManifestResource(new StringAsset("path-prefix('/foo') -> rewrite('/bar')"),
                            "undertow-handlers.conf"));

    @Test
    public void testUndertowHandlersRewrite() {
        when().get("/foo").then()
                .statusCode(200)
                .body(is("RedirectedServlet"));
    }

    @WebServlet(urlPatterns = "/bar")
    public static class RedirectedServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().print("RedirectedServlet");
        }
    }

}
