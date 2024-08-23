package io.quarkus.undertow.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.undertow.deployment.ServletBuildItem;

public class MultipartConfigTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultipartServlet.class))
            .addBuildChainCustomizer(b -> {
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(ServletBuildItem.builder("Test Servlet", MultipartServlet.class.getName())
                                .addMapping("/servlet-item")
                                .setMultipartConfig(new MultipartConfigElement(""))
                                .build());
                    }
                }).produces(ServletBuildItem.class).build();
            });

    @ParameterizedTest
    @ValueSource(strings = { "/foo", "/servlet-item" })
    public void testMultipartConfig(String path) {
        given().multiPart("file", "random.txt", "Some random file".getBytes(StandardCharsets.UTF_8))
                .when().post(path).then()
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
