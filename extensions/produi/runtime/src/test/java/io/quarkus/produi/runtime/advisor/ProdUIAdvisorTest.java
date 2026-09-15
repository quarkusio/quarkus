package io.quarkus.produi.runtime.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.produi.runtime.advisor.ProdUIAdvisor.Check;
import io.quarkus.produi.runtime.advisor.ProdUIAdvisor.Status;

class ProdUIAdvisorTest {

    @Test
    void prodUiSecuredPassesWhenRolesConfigured() {
        assertThat(ProdUIAdvisor.prodUiSecured(true).status()).isEqualTo(Status.PASS);
        assertThat(ProdUIAdvisor.prodUiSecured(false).status()).isEqualTo(Status.WARN);
    }

    @Test
    void managementAuthFailsWhenEnabledButUnsecured() {
        assertThat(ProdUIAdvisor.managementInterfaceAuth(true, false).status()).isEqualTo(Status.FAIL);
        assertThat(ProdUIAdvisor.managementInterfaceAuth(true, true).status()).isEqualTo(Status.PASS);
        // A disabled management interface is not a finding.
        assertThat(ProdUIAdvisor.managementInterfaceAuth(false, false).status()).isEqualTo(Status.PASS);
    }

    @Test
    void plaintextSecretsWarnsAndListsNamesOnly() {
        Check none = ProdUIAdvisor.plaintextSecrets(List.of());
        assertThat(none.status()).isEqualTo(Status.PASS);

        Check found = ProdUIAdvisor.plaintextSecrets(List.of("quarkus.datasource.password", "app.api-key"));
        assertThat(found.status()).isEqualTo(Status.WARN);
        assertThat(found.detail()).contains("quarkus.datasource.password", "app.api-key");
    }

    @Test
    void nullListsAreTreatedAsEmpty() {
        assertThat(ProdUIAdvisor.plaintextSecrets(null).status()).isEqualTo(Status.PASS);
        assertThat(ProdUIAdvisor.devFeaturesInProduction(null).status()).isEqualTo(Status.PASS);
    }

    @Test
    void devFeaturesWarnsWhenRiskySettingsPresent() {
        assertThat(ProdUIAdvisor.devFeaturesInProduction(List.of()).status()).isEqualTo(Status.PASS);
        Check found = ProdUIAdvisor
                .devFeaturesInProduction(List.of("quarkus.hibernate-orm.database.generation"));
        assertThat(found.status()).isEqualTo(Status.WARN);
        assertThat(found.detail()).contains("quarkus.hibernate-orm.database.generation");
    }

    @Test
    void scoreWeightsWarnAsHalfAndFailAsZero() {
        assertThat(ProdUIAdvisor.score(List.of())).isEqualTo(100);
        Check pass = ProdUIAdvisor.prodUiSecured(true);
        Check warn = ProdUIAdvisor.prodUiSecured(false);
        Check fail = ProdUIAdvisor.managementInterfaceAuth(true, false);
        assertThat(ProdUIAdvisor.score(List.of(pass, pass))).isEqualTo(100);
        assertThat(ProdUIAdvisor.score(List.of(pass, fail))).isEqualTo(50);
        assertThat(ProdUIAdvisor.score(List.of(pass, warn))).isEqualTo(75);
        assertThat(ProdUIAdvisor.score(List.of(fail, fail))).isEqualTo(0);
    }

    @Test
    void checksAggregateInDisplayOrder() {
        List<Check> checks = ProdUIAdvisor.checks(true, true, true, List.of(), List.of());
        assertThat(checks).extracting(Check::id)
                .containsExactly("prod-ui-secured", "management-auth", "plaintext-secrets", "dev-features");
    }
}
