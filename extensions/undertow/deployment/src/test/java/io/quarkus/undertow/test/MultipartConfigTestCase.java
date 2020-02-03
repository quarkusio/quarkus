package io.quarkus.undertow.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.codec.Charsets;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MultipartConfigTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultipartServlet.class));

    @Test
    public void testMultipartConfig() {
        given().multiPart("file", "random.txt", "Some random file".getBytes(Charsets.UTF_8))
                .when().post("/foo").then()
                .statusCode(201)
                .body(is("OK"));
    }

    @WebServlet("/foo")
    @MultipartConfig()
    public static class MultipartServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            Part p = request.getPart("file");
            System.out.println(p.getName());
            response.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter out = response.getWriter()) {
                out.print("OK");
            }
        }
    }

}
