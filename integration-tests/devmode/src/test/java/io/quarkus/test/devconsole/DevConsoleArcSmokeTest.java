package io.quarkus.test.devconsole;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleArcSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Foo.class));

    @Test
    public void testPages() {
        RestAssured
                .get("q/dev/io.quarkus.quarkus-arc/beans")
                .then()
                .statusCode(200).body(Matchers.containsString("io.quarkus.test.devconsole.DevConsoleArcSmokeTest$Foo"));
        RestAssured
                .get("q/dev/io.quarkus.quarkus-arc/observers")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("io.quarkus.test.devconsole.DevConsoleArcSmokeTest$Foo#onStr"));
        RestAssured
                .get("q/dev/io.quarkus.quarkus-arc/events")
                .then()
                .statusCode(200).body(Matchers.containsString("io.quarkus.runtime.StartupEvent"));
        RestAssured
                .get("q/dev/io.quarkus.quarkus-arc/invocations")
                .then()
                .statusCode(200);
        RestAssured
                .get("q/dev/io.quarkus.quarkus-arc/removed-beans")
                .then()
                .statusCode(200).body(Matchers.containsString("org.jboss.logging.Logger"));
    }

    @Named
    @ApplicationScoped
    public static class Foo {

        void onStr(@Observes String event) {
        }

    }
}
