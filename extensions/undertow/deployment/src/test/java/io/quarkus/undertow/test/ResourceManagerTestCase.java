package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ResourceManagerTestCase {

    private static final String CONTEXT_PATH = "/foo";
    public static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(ContextPathServlet.class)
                    .addAsResource(new StringAsset("index.html"), "META-INF/resources/index.html")
                    .addAsResource(new StringAsset("foo/foo.html"), "META-INF/resources/foo/foo.html")
                    .addAsResource(new StringAsset("foo/bar/bar.html"), "META-INF/resources/foo/bar/bar.html"));

    @Test
    public void testServlet() {
        RestAssured.when().get("/").then().statusCode(200).body(is("[foo, index.html]"));
        RestAssured.when().get("/foo").then().statusCode(200).body(is("[foo/bar, foo/foo.html]"));
        RestAssured.when().get("/foo/bar").then().statusCode(200).body(is("[foo/bar/bar.html]"));
    }

    @WebServlet(urlPatterns = "/*")
    public static class ContextPathServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            var paths = req.getServletContext().getResourcePaths(req.getPathInfo() == null ? "/" : req.getPathInfo());
            resp.getWriter()
                    .write(String.valueOf(new TreeSet<>(paths.stream()
                            .map(s -> s.substring(s.lastIndexOf(META_INF_RESOURCES) + META_INF_RESOURCES.length()))
                            .collect(Collectors.toSet()))));
        }
    }
}
