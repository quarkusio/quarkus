package io.quarkus.undertow.test.devmode;

import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ServletChangeTestCase {

    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(DevServlet.class)
                            .addAsManifestResource(new StringAsset("Hello Resource"), "resources/file.txt");
                }
            });

    @Test
    public void testServletChange() throws InterruptedException, ExecutionException {
        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello World"));

        test.modifySourceFile("DevServlet.java", new Function<String, String>() {

            @Override
            public String apply(String s) {
                return s.replace("Hello World", "Hello Quarkus");
            }
        });

        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));

        test.modifySourceFile("DevServlet.java", new Function<String, String>() {

            @Override
            public String apply(String s) {
                return s.replace("/dev", "/new");
            }
        });

        RestAssured.when().get("/dev").then()
                .statusCode(404);

        RestAssured.when().get("/new").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));

        ExecutorService service = Executors.newFixedThreadPool(20);
        List<Future<Object>> results = new ArrayList<>();
        //make sure we are always dispatched
        //https://github.com/quarkusio/quarkus/issues/7782
        for (int i = 0; i < 1000; ++i) {
            results.add(service.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    RestAssured.when().get("/new").then()
                            .statusCode(200)
                            .body(is("Hello Quarkus"));
                    return null;
                }
            }));
        }
        for (Future<Object> i : results) {
            i.get();
        }
    }

    @Test
    public void testAddServlet() throws InterruptedException {
        RestAssured.when().get("/new").then()
                .statusCode(404);

        test.addSourceFile(NewServlet.class);

        RestAssured.when().get("/new").then()
                .statusCode(200)
                .body(is("A new Servlet"));
    }

    @Test
    public void testResourceChange() throws InterruptedException {
        RestAssured.when().get("/file.txt").then()
                .statusCode(200)
                .body(is("Hello Resource"));

        test.modifyResourceFile("META-INF/resources/file.txt", new Function<String, String>() {

            @Override
            public String apply(String s) {
                return "A new resource";
            }
        });

        RestAssured.when().get("file.txt").then()
                .statusCode(200)
                .body(is("A new resource"));
    }

    @Test
    public void testAddResource() throws InterruptedException {

        RestAssured.when().get("/new.txt").then()
                .statusCode(404);

        test.addResourceFile("META-INF/resources/new.txt", "New File");

        RestAssured.when().get("/new.txt").then()
                .statusCode(200)
                .body(is("New File"));

    }
}
