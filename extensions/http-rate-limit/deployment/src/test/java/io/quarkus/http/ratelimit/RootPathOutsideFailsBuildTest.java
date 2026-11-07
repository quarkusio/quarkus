package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Rule paths include {@code quarkus.http.root-path}, so a rule path outside it matches no request: the limit would
 * silently never apply, which fails the build. Paths under the root path, a wildcard over it, and paths under the
 * non-application root path are accepted; an exact path that is a parent of the root path is not.
 */
public class RootPathOutsideFailsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(TestHttp.ipRule(TestHttp.ipRule(TestHttp.ipRule(
            TestHttp.ipRule(new QuarkusExtensionTest(), "outside", "/api/*,/apps", 1, "1m"), "inside",
            "/app/api/*,/app", 1, "1m"), "everything", "/*", 1, "1m"), "management", "/app/q/x", 1, "1m"),
            "parent", "/", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class))
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'outside' path '/api/*' is outside quarkus.http.root-path"
                            + " '/app/', so it matches no request; write it under the root path")
                    .hasMessageContaining("Rate limit rule 'outside' path '/apps' is outside quarkus.http.root-path"
                            + " '/app/', so it matches no request; write it under the root path")
                    // an exact path matches only itself, even when it is a parent of the root path
                    .hasMessageContaining("Rate limit rule 'parent' path '/' is outside quarkus.http.root-path"
                            + " '/app/', so it matches no request; write it under the root path")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain("'inside'", "'everything'",
                            "'management'")));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
