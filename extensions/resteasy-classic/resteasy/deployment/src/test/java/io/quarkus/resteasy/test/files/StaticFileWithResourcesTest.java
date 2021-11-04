package io.quarkus.resteasy.test.files;

import static org.hamcrest.CoreMatchers.containsString;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.test.RootResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test that static files are served even with resources.
 */
public class StaticFileWithResourcesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RootResource.class)
                    .addAsResource(new File("src/test/resources/lorem.txt"), "META-INF/resources/lorem.txt")
                    .addAsResource(new File("src/test/resources/index.html"), "META-INF/resources/web/index.html"));

    @Test
    public void test() {
        RestAssured.get("/").then()
                .statusCode(200)
                .body(containsString("Root Resource"));

        RestAssured.get("/web/index.html").then()
                .statusCode(200)
                .body(containsString("<h1>Hello</h1>"));

        RestAssured.get("/web/").then()
                .statusCode(200)
                .body(containsString("<h1>Hello</h1>"));

        RestAssured.get("/lorem.txt").then()
                .statusCode(200)
                .body(containsString("Lorem"));
    }

    /**
     * Tests that multiple simultaneous requests to a static resource, with each request
     * using the {@code If-Modified-Since} header, doesn't cause server side errors.
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/4627"/>
     */
    @Test
    public void testMultipleThreadedRequestWithIfModifiedSince() throws Exception {
        // RFC1123 date formatter
        final DateFormat dtf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        dtf.setTimeZone(TimeZone.getTimeZone("GMT"));
        // date in past, so that we always get a 200 response, instead of 304
        final Date fiveMinInPast = new Date(System.currentTimeMillis() - (5 * 60 * 1000));
        final String modifiedSinceHeader = dtf.format(fiveMinInPast);
        final int numRequests = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(numRequests);
        try {
            final List<Future<Void>> results = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                // issue the requests
                results.add(executorService.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        RestAssured.given().header("If-Modified-Since", modifiedSinceHeader)
                                .get("/web/index.html")
                                .then()
                                .body(containsString("<h1>Hello</h1>"))
                                .statusCode(200);
                        return null;
                    }
                }));
            }
            // wait for completion
            for (int i = 0; i < numRequests; i++) {
                results.get(i).get(1, TimeUnit.MINUTES);
            }
        } finally {
            executorService.shutdownNow();
        }
    }
}
