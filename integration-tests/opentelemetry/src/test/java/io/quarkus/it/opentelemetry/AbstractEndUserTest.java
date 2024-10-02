package io.quarkus.it.opentelemetry;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.quarkus.it.opentelemetry.AbstractEndUserTest.User.SCOTT;
import static io.quarkus.it.opentelemetry.AbstractEndUserTest.User.STUART;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.semconv.SemanticAttributes;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractEndUserTest {

    private static final String HTTP_PERM_AUGMENTOR_ROLE = "HTTP-PERM-AUGMENTOR";
    private static final String END_USER_ID_ATTR = "attr_" + SemanticAttributes.ENDUSER_ID.getKey();
    private static final String END_USER_ROLE_ATTR = "attr_" + SemanticAttributes.ENDUSER_ROLE.getKey();
    private static final String READER_ROLE = "READER";
    private static final String WRITER_ROLE = "WRITER";
    private static final String WRITER_HTTP_PERM_ROLE = "WRITER-HTTP-PERM";
    private static final String AUTH_FAILURE_ROLE = "AUTHZ-FAILURE-ROLE";
    private static final String AUGMENTOR_ROLE = "AUGMENTOR";

    /**
     * This is 'ROLES-ALLOWED-MAPPING-ROLE' role granted to the SecureIdentity by augmentor and
     * remapped to 'ROLES-ALLOWED-MAPPING-ROLE-HTTP-PERM' role which allows to verify that the
     * 'quarkus.http.auth.roles-mapping' config-level roles mapping is reflected in the End User attributes.
     */
    private static final String HTTP_PERM_ROLES_ALLOWED_MAPPING_ROLE = "ROLES-ALLOWED-MAPPING-ROLE-HTTP-PERM";

    @BeforeEach
    @AfterEach
    public void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, SECONDS).until(() -> getSpans().isEmpty());
    }

    @Test
    public void testAttributesWhenNoAuthorizationInPlace() {
        var subPath = "/no-authorization";
        request(subPath, SCOTT).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(SCOTT, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(READER_ROLE));
            assertFalse(roles.contains(WRITER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testWhenRolesAllowedAnnotationOnlyUnauthorized() {
        var subPath = "/roles-allowed-only-writer-role";
        // the endpoint is annotated with @RolesAllowed("WRITER") and no other authorization is in place
        request(subPath, SCOTT).statusCode(403);

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testWhenRolesAllowedAnnotationOnlyAuthorized() {
        var subPath = "/roles-allowed-only-writer-role";
        // the endpoint is annotated with @RolesAllowed("WRITER") and no other authorization is in place
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(STUART, spanData);
        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertTrue(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testWhenPermitAllOnly() {
        var subPath = "/permit-all-only";
        // request to endpoint with @PermitAll annotation
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(STUART, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(READER_ROLE));
            assertTrue(roles.contains(WRITER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testWhenRolesAllowedAnnotationOnlyAuthorizedAugmentor() {
        var subPath = "/roles-allowed-only-augmentor-role";
        // the endpoint is annotated with @RolesAllowed("AUGMENTOR") and no other authorization is in place
        request(subPath, SCOTT).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(AUGMENTOR_ROLE));
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testWhenPermitAllOnlyAugmentor() {
        var subPath = "/permit-all-only-augmentor";
        // the endpoint is annotated with @PermitAll and no authorization is in place
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(STUART, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(AUGMENTOR_ROLE));
            assertTrue(roles.contains(READER_ROLE));
            assertTrue(roles.contains(WRITER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testAttributesWhenNoAuthorizationInPlaceAugmentor() {
        var subPath = "/no-authorization-augmentor";
        // there is no authorization in place, therefore authentication happnes on demand
        request(subPath, SCOTT).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(SCOTT, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(AUGMENTOR_ROLE));
            assertTrue(roles.contains(READER_ROLE));
            assertFalse(roles.contains(WRITER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testWhenConfigRolesMappingAndHttpPermAugmentor() {
        var subPath = "/roles-mapping-http-perm-augmentor";
        // the endpoint is annotated with @PermitAll, HTTP permission 'permit-all' is in place; auth happens on demand
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(STUART, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(AUGMENTOR_ROLE));
            assertTrue(roles.contains(HTTP_PERM_ROLES_ALLOWED_MAPPING_ROLE));
            assertTrue(roles.contains(READER_ROLE));
            assertTrue(roles.contains(WRITER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testWhenConfigRolesMappingHttpPerm() {
        var subPath = "/roles-mapping-http-perm";
        // request endpoint with both 'permit-all' HTTP permission and @PermitAll annotation
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        if (isProactiveAuthEnabled()) {
            assertEndUserId(STUART, spanData);

            var roles = getRolesAttribute(spanData);
            assertTrue(roles.contains(HTTP_PERM_ROLES_ALLOWED_MAPPING_ROLE));
            assertTrue(roles.contains(WRITER_ROLE));
            assertTrue(roles.contains(READER_ROLE));
        } else {
            assertNoEndUserAttributes(spanData);
        }
    }

    @Test
    public void testWhenRolesAllowedAnnotationHttpPermUnauthorized() {
        var subPath = "/roles-allowed-writer-http-perm-role";
        // the endpoint is annotated with @RolesAllowed("WRITER-HTTP-PERM")
        // the 'AUTHZ-FAILURE-ROLE' mapped from 'READER' by HTTP permission roles policy
        request(subPath, SCOTT).statusCode(403);

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertTrue(roles.contains(AUTH_FAILURE_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
        assertFalse(roles.contains(WRITER_HTTP_PERM_ROLE));
    }

    @Test
    public void testWhenRolesAllowedAnnotationHttpPermAuthorized() {
        var subPath = "/roles-allowed-writer-http-perm-role";
        // the endpoint is annotated with @RolesAllowed("WRITER-HTTP-PERM")
        // the 'WRITER-HTTP-PERM' role is remapped from 'WRITER' role by HTTP permission roles policy
        // the 'AUTHZ-FAILURE-ROLE' mapped from 'READER' by HTTP permission roles policy
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(STUART, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertTrue(roles.contains(AUTH_FAILURE_ROLE));
        assertTrue(roles.contains(WRITER_ROLE));
        assertTrue(roles.contains(WRITER_HTTP_PERM_ROLE));
    }

    @Test
    public void testWhenRolesAllowedAnnotationHttpPermAuthorizedAugmentor() {
        var subPath = "/roles-allowed-http-perm-augmentor-role";
        // the endpoint is annotated with @RolesAllowed("HTTP-PERM-AUGMENTOR")
        // and role 'HTTP-PERM-AUGMENTOR' is mapped by HTTP perm roles policy from the 'AUGMENTOR' role
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(STUART, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(AUGMENTOR_ROLE));
        assertTrue(roles.contains(HTTP_PERM_AUGMENTOR_ROLE));
        assertTrue(roles.contains(WRITER_ROLE));
        assertTrue(roles.contains(READER_ROLE));
    }

    @Test
    public void testJaxRsHttpPermOnlyAuthorized() {
        var subPath = "/jax-rs-http-perm";
        // only JAX-RS HTTP Permission roles policy that requires 'WRITER' role is in place
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(STUART, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertTrue(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testJaxRsHttpPermOnlyUnauthorized() {
        var subPath = "/jax-rs-http-perm";
        // only JAX-RS HTTP Permission roles policy that requires 'WRITER' role is in place
        request(subPath, SCOTT).statusCode(403);

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testJaxRsHttpPermAndRolesAllowedAnnotationAuthorized() {
        var subPath = "/jax-rs-http-perm-annotation-reader-role";
        // both JAX-RS HTTP Permission roles policy that requires 'WRITER' role and @RolesAllowed("READER") are in place
        request(subPath, STUART).statusCode(200).body(is(subPath));

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(STUART, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertTrue(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testJaxRsHttpPermAndRolesAllowedAnnotationUnauthorized() {
        var subPath = "/jax-rs-http-perm-annotation-reader-role";
        // both JAX-RS HTTP Permission roles policy that requires 'WRITER' role and @RolesAllowed("READER") are in place
        request(subPath, SCOTT).statusCode(403);

        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
    }

    @Test
    public void testCustomSpanContainsEndUserAttributes() {
        var subPath = "/custom-span-reader-role";
        // the endpoint is annotated with @RolesAllowed("READER") and no other authorization is in place
        request(subPath, SCOTT).statusCode(200).body(is(subPath));
        var spanData = waitForSpanWithSubPath(subPath);

        assertEndUserId(SCOTT, spanData);

        var roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));

        // assert custom span also contains end user attributes
        spanData = waitForSpanWithPath("custom-path");
        assertEquals("custom-value", spanData.get("attr_custom_attribute"), spanData.toString());

        assertEndUserId(SCOTT, spanData);

        roles = getRolesAttribute(spanData);
        assertTrue(roles.contains(READER_ROLE));
        assertFalse(roles.contains(WRITER_ROLE));
    }

    protected abstract boolean isProactiveAuthEnabled();

    enum User {

        SCOTT("reader"), // READER_ROLE
        STUART("writer"); // READER_ROLE, WRITER_ROLE

        private final String password;

        User(String password) {
            this.password = password;
        }

        private String userName() {
            return this.toString().toLowerCase();
        }
    }

    private static void assertEndUserId(User requestUser, Map<String, Object> spanData) {
        assertEquals(requestUser.userName(), spanData.get(END_USER_ID_ATTR), spanData.toString());
    }

    private static void assertNoEndUserAttributes(Map<String, Object> spanData) {
        assertNull(spanData.get(END_USER_ID_ATTR), spanData.toString());
        assertNull(spanData.get(END_USER_ROLE_ATTR), spanData.toString());
    }

    private static String getRolesAttribute(Map<String, Object> spanData) {
        var roles = (String) spanData.get(END_USER_ROLE_ATTR);
        assertNotNull(roles, spanData.toString());
        return roles;
    }

    private static ValidatableResponse request(String subPath, User requestUser) {
        return given()
                .when()
                .auth().preemptive().basic(requestUser.userName(), requestUser.password)
                .get(createResourcePath(subPath))
                .then();
    }

    private static String createResourcePath(String subPath) {
        return "/otel/enduser" + subPath;
    }

    private static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    private static Map<String, Object> getSpanByPath(final String path) {
        return getSpans()
                .stream()
                .filter(m -> path.equals(m.get("attr_" + URL_PATH.getKey())))
                .findFirst()
                .orElse(Map.of());
    }

    private static Map<String, Object> waitForSpanWithSubPath(final String subPath) {
        return waitForSpanWithPath(createResourcePath(subPath));
    }

    private static Map<String, Object> waitForSpanWithPath(final String path) {
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> !getSpanByPath(path).isEmpty(), new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Boolean aBoolean) {
                return Boolean.TRUE.equals(aBoolean);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Span with the 'url.path' attribute not found: " + path + " ; " + getSpans());
            }
        });
        return getSpanByPath(path);
    }
}
