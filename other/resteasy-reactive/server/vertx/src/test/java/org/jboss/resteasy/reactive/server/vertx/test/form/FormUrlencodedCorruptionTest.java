package org.jboss.resteasy.reactive.server.vertx.test.form;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import java.net.URLEncoder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FormUrlencodedCorruptionTest {
    @RegisterExtension
    static ResteasyReactiveUnitTest TEST = new ResteasyReactiveUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class));

    @Test
    public void testBound() {
        testWithUri("/test/bound");
    }

    @Test
    public void testUnbound() {
        testWithUri("/test/unbound");
    }

    @Test
    public void testMixed() {
        testWithUri("/test/mixed");
    }

    private void testWithUri(String uri) {
        String first = "Gaius Julius Caesar";
        String next = "Gnaeus Pompeius Magnus";
        String last = "Marcus Licinius Crassus";
        String body = encode("first", first, "next", next, "last", last);
        RestAssured
                .given()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .post(uri)
                .then()
                .statusCode(200)
                .body(is(String.join("\n", first, next, last)));
    }

    private static String encode(String... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i += 2) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(args[i], UTF_8)).append('=').append(URLEncoder.encode(args[i + 1], UTF_8));
        }
        return sb.toString();
    }

    @Path("test")
    public static class TestResource {
        @Path("bound")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String bound(@RestForm String first, @RestForm String next, @RestForm String last) {
            return String.join("\n", first, next, last);
        }

        @Path("unbound")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String unbound(MultivaluedMap<String, String> params) {
            return String.join("\n", params.getFirst("first"), params.getFirst("next"), params.getFirst("last"));
        }

        @Path("mixed")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String mixed(@RestForm String first, MultivaluedMap<String, String> params) {
            return String.join("\n", first, params.getFirst("next"), params.getFirst("last"));
        }
    }
}
