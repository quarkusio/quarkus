package io.quarkus.produi.runtime.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

/**
 * Unit tests for {@link EndpointsProdUIService#setHttpInfo} - the non-sensitive port/root-path the UI uses to
 * build absolute endpoint links to the application interface (Prod UI itself is served on the management interface).
 */
class EndpointsProdUIServiceTest {

    @Test
    void defaultsToRootPathAndNoPort() {
        EndpointsProdUIService service = new EndpointsProdUIService();
        JsonObject info = service.getHttpInfo();
        assertThat(info.getString("rootPath")).isEqualTo("/");
        assertThat(info.containsKey("port")).isFalse();
    }

    @Test
    void recordsPortAndRootPath() {
        EndpointsProdUIService service = new EndpointsProdUIService();
        service.setHttpInfo(8080, "/api");
        JsonObject info = service.getHttpInfo();
        assertThat(info.getInteger("port")).isEqualTo(8080);
        assertThat(info.getString("rootPath")).isEqualTo("/api");
    }

    @Test
    void omitsPortWhenNull() {
        EndpointsProdUIService service = new EndpointsProdUIService();
        service.setHttpInfo(null, "/");
        JsonObject info = service.getHttpInfo();
        assertThat(info.containsKey("port")).isFalse();
        assertThat(info.getString("rootPath")).isEqualTo("/");
    }

    @Test
    void normalizesNullOrEmptyRootPathToSlash() {
        EndpointsProdUIService service = new EndpointsProdUIService();
        service.setHttpInfo(9000, null);
        assertThat(service.getHttpInfo().getString("rootPath")).isEqualTo("/");
        service.setHttpInfo(9000, "");
        assertThat(service.getHttpInfo().getString("rootPath")).isEqualTo("/");
    }
}
