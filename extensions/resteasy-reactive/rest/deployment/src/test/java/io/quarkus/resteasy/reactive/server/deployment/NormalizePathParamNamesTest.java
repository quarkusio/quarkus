package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.resteasy.reactive.server.deployment.ResteasyReactiveProcessor.normalizePathParamNames;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizePathParamNamesTest {

    @Test
    void noParams() {
        assertThat(normalizePathParamNames("/v1/items")).isEqualTo("/v1/items");
    }

    @Test
    void singleParam() {
        assertThat(normalizePathParamNames("/v1/{id}")).isEqualTo("/v1/{*}");
        assertThat(normalizePathParamNames("/v1/{product}")).isEqualTo("/v1/{*}");
    }

    @Test
    void multipleParams() {
        assertThat(normalizePathParamNames("/v1/{parentId}/items/{childId}"))
                .isEqualTo("/v1/{*}/items/{*}");
    }

    @Test
    void customRegex() {
        assertThat(normalizePathParamNames("/v1/{id:\\d+}")).isEqualTo("/v1/{*:\\d+}");
        assertThat(normalizePathParamNames("/v1/{name:\\d+}")).isEqualTo("/v1/{*:\\d+}");
    }

    @Test
    void customRegexWithNestedBraces() {
        assertThat(normalizePathParamNames("/v1/{id:\\d{2,4}}")).isEqualTo("/v1/{*:\\d{2,4}}");
    }

    @Test
    void differentParamNamesSameStructure() {
        String a = normalizePathParamNames("/v1/{parentId}");
        String b = normalizePathParamNames("/v1/{product}");
        assertThat(a).isEqualTo(b);
    }

    // Different custom regex patterns match different URL sets, so they are not duplicates
    @Test
    void differentRegexSameParamName() {
        String a = normalizePathParamNames("/v1/{id:\\d+}");
        String b = normalizePathParamNames("/v1/{id:[a-z]+}");
        assertThat(a).isNotEqualTo(b);
    }
}
