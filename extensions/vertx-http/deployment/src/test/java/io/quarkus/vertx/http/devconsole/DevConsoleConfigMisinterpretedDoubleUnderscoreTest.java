package io.quarkus.vertx.http.devconsole;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Tests that a system property such as {@code %.intellij.command.histfile."}
 * doesn't lead to an exception because {@code "} is incorrectly seen as a quoted property.
 * <p>
 * Originally the bug stemmed from an environment property {@code __INTELLIJ_COMMAND_HISTFILE__}
 * which was (weirdly) interpreted as {@code %.intellij.command.histfile."},
 * but it's much easier to test system properties (which are mutable)
 * than environment properties.
 */
public class DevConsoleConfigMisinterpretedDoubleUnderscoreTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setBuildSystemProperty("%.intellij.command.histfile.\"", "foo")
            .withEmptyApplication();

    @Test
    public void testNoFailure() {
        RestAssured.get("q/dev/io.quarkus.quarkus-vertx-http/config")
                .then()
                .statusCode(200).body(Matchers.containsString("Config Editor"));
    }
}
