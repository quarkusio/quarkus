package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests how the non-application root path is derived from configuration. The root path is
 * normalized by {@code NormalizeRootHttpPathConverter} and therefore always has both a leading
 * and a trailing {@code /}, while the non-application root path is not normalized at all, so
 * the two have to be combined carefully.
 */
public class ManagementRequestPrioritizerPathTest {
    @Test
    public void relativeToTheDefaultRootPath() {
        assertThat(path("/", "q")).isEqualTo("/q");
        assertThat(path("/", "q/")).isEqualTo("/q");
        assertThat(path("/", "management")).isEqualTo("/management");
    }

    @Test
    public void relativeToACustomRootPath() {
        assertThat(path("/app/", "q")).isEqualTo("/app/q");
        assertThat(path("/app/", "q/")).isEqualTo("/app/q");
    }

    @Test
    public void absoluteIgnoresTheRootPath() {
        assertThat(path("/", "/q")).isEqualTo("/q");
        assertThat(path("/app/", "/q")).isEqualTo("/q");
        assertThat(path("/app/", "/q/")).isEqualTo("/q");
    }

    // setting the non-application root path to the root path disables it, and then every
    // endpoint is an application endpoint and none may be exempt from load shedding
    @Test
    public void sameAsRootPathIsNotRecognizable() {
        assertThat(path("/", "/")).isNull();
        assertThat(path("/", "")).isNull();
        assertThat(path("/app/", "/app/")).isNull();
        assertThat(path("/app/", "/app")).isNull();
        assertThat(path("/app/", "")).isNull();
    }

    private static String path(String rootPath, String nonApplicationRootPath) {
        return ManagementRequestPrioritizer.nonApplicationPath(rootPath, nonApplicationRootPath);
    }
}
