package io.quarkus.it.panache.reactive.sessionrestart;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * Reproduces issue https://github.com/quarkusio/quarkus/issues/56935. The three classes in this package share a test-name
 * prefix. Run just them from the Quarkus
 * repository root:
 *
 * <pre>
 * ./mvnw -f integration-tests/hibernate-reactive-panache/ '-Dtest=SessionFactoryRestart0*Test' -Dtest-containers -Dstart-containers test
 * </pre>
 *
 * <ol>
 * <li>This class starts Quarkus with a class-restricted test resource and caches the session factory.</li>
 * <li>{@link SessionFactoryRestart02NoResourceTest} has no test resource, so Quarkus restarts to remove the
 * class-restricted resource.</li>
 * <li>{@link SessionFactoryRestart03ResourceAgainTest} adds the class-restricted resource back, causing another restart.
 * Before the cache is cleared at shutdown, its Panache call uses the stale session factory and fails with
 * <code>UnknownServiceException: ReactiveConnectionPool</code>. The test asserts that the call succeeds after the fix.</li>
 * </ol>
 *
 * The restart is caused by {@code restrictToAnnotatedClass = true}, which makes Quarkus re-augment and restart the
 * application when moving between classes with different test-resource sets. The particular resource type is not important.
 *
 * The numeric class-name prefix preserves this order under Quarkus's default class-name orderer in the full test suite.
 */
@QuarkusTest
@TestProfile(SessionFactoryRestart01ResourceTest.SessionFactoryRestartProfile.class)
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true)
public class SessionFactoryRestart01ResourceTest {

    @Test
    public void testPanacheSessionBeforeRestart() {
        RestAssured.when().get("/test-transactional/panache-session")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    public static class SessionFactoryRestartProfile implements QuarkusTestProfile {
    }
}
