package io.quarkus.produi.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.junit.jupiter.api.Test;

class ProdUISecurityTest {

    @Test
    void noRolesMeansNoAuthorization() {
        assertThat(ProdUISecurity.authConfigDefaults("quarkus.management.auth", "/q/prod-ui", List.of())).isEmpty();
        assertThat(ProdUISecurity.authConfigDefaults("quarkus.management.auth", "/q/prod-ui", null)).isEmpty();
    }

    @Test
    void bindsManagementPermissionToProdUiPaths() {
        var config = ProdUISecurity.authConfigDefaults("quarkus.management.auth", "/q/prod-ui", List.of("admin", "ops"));
        assertThat(config).containsExactly(
                // The exact base path AND everything under it - the static assets and the json-rpc-ws data plane.
                entry("quarkus.management.auth.permission.quarkus-prod-ui.paths", "/q/prod-ui,/q/prod-ui/*"),
                entry("quarkus.management.auth.permission.quarkus-prod-ui.policy", "quarkus-prod-ui"),
                entry("quarkus.management.auth.policy.quarkus-prod-ui.roles-allowed", "admin,ops"));
    }

    @Test
    void supportsMainInterfacePrefix() {
        var config = ProdUISecurity.authConfigDefaults("quarkus.http.auth", "/q/prod-ui", List.of("admin"));
        assertThat(config).containsKeys(
                "quarkus.http.auth.permission.quarkus-prod-ui.paths",
                "quarkus.http.auth.permission.quarkus-prod-ui.policy",
                "quarkus.http.auth.policy.quarkus-prod-ui.roles-allowed");
        assertThat(config).containsEntry("quarkus.http.auth.policy.quarkus-prod-ui.roles-allowed", "admin");
    }

    @Test
    void normalisesTrailingSlashInPath() {
        var config = ProdUISecurity.authConfigDefaults("quarkus.management.auth", "/q/prod-ui/", List.of("admin"));
        assertThat(config).containsEntry("quarkus.management.auth.permission.quarkus-prod-ui.paths",
                "/q/prod-ui,/q/prod-ui/*");
    }
}
