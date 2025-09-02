package io.quarkus.undertow.test.sessioncontext;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

public class SessionContextTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addClasses(TestServlet.class, Foo.class, ObservingBean.class, SessionScopedObserver.class));

    @Test
    public void testServlet() {
        Response response = when().get("/foo");
        String sessionId = response.sessionId();
        response.then().statusCode(200).body(is("count=1"));
        given().sessionId(sessionId).when().get("/foo").then().statusCode(200).body(is("count=2"));
        // Destroy session
        when().get("/foo?destroy=true").then().statusCode(200);
        response = when().get("/foo");
        assertNotEquals(sessionId, response.sessionId());
        response.then().statusCode(200).body(is("count=1"));
    }

    @Test
    public void testContextEvents() {
        ObservingBean observingBean = Arc.container().select(ObservingBean.class).get();

        // make sure we start with zero events to keep this test method independent
        observingBean.resetState();
        SessionScopedObserver.resetState();

        // following request creates a session and also destroys it by enforcing invalidation
        when().get("/foo?destroy=true").then().statusCode(200);
        Assertions.assertEquals(1, observingBean.getTimesInitObserved());
        Assertions.assertEquals(1, observingBean.getTimesBeforeDestroyedObserved());
        Assertions.assertEquals(1, observingBean.getTimesDestroyedObserved());

        // assert that @SessionScoped bean can observe init and before destroyed events as well
        Assertions.assertEquals(1, SessionScopedObserver.timesInitObserved);
        Assertions.assertEquals(1, SessionScopedObserver.timesBeforeDestroyedObserved);
    }

}
