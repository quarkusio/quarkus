package io.quarkus.produi.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretMaskerTest {

    @Test
    void detectsSecretsByName() {
        assertThat(SecretMasker.isSecretName("quarkus.oidc.credentials.secret")).isTrue();
        assertThat(SecretMasker.isSecretName("quarkus.datasource.password")).isTrue();
        assertThat(SecretMasker.isSecretName("REGISTRY_PASSWORD")).isTrue();
        assertThat(SecretMasker.isSecretName("some.api-key")).isTrue();
        assertThat(SecretMasker.isSecretName("app.access-key-id")).isTrue();
        assertThat(SecretMasker.isSecretName("my.auth.token")).isTrue();
    }

    @Test
    void doesNotFlagOrdinaryNames() {
        assertThat(SecretMasker.isSecretName("quarkus.http.port")).isFalse();
        assertThat(SecretMasker.isSecretName("quarkus.datasource.jdbc.url")).isFalse();
        assertThat(SecretMasker.isSecretName(null)).isFalse();
    }

    @Test
    void detectsInlineAuthorizationInValues() {
        assertThat(SecretMasker.isSecretValue("Authorization=Bearer super-secret-otlp-token")).isTrue();
        assertThat(SecretMasker.isSecretValue("Bearer abc.def.ghi")).isTrue();
        assertThat(SecretMasker.isSecretValue("Basic dXNlcjpwYXNz")).isTrue();
        assertThat(SecretMasker.isSecretValue("localhost:9200")).isFalse();
        assertThat(SecretMasker.isSecretValue("")).isFalse();
        assertThat(SecretMasker.isSecretValue(null)).isFalse();
    }

    @Test
    void masksSecretValuesButKeepsOrdinaryOnes() {
        assertThat(SecretMasker.maskIfSecret("quarkus.oidc.credentials.secret", "super-secret"))
                .isEqualTo(SecretMasker.MASK);
        assertThat(SecretMasker.maskIfSecret("quarkus.otel.exporter.otlp.headers", "Authorization=Bearer tok"))
                .isEqualTo(SecretMasker.MASK);
        assertThat(SecretMasker.maskIfSecret("quarkus.http.port", "8080")).isEqualTo("8080");
    }

    @Test
    void doesNotMaskEmptySecrets() {
        // An empty secret has nothing to hide; keep it empty rather than showing a fake mask.
        assertThat(SecretMasker.maskIfSecret("quarkus.datasource.password", "")).isEmpty();
        assertThat(SecretMasker.maskIfSecret("quarkus.datasource.password", null)).isEmpty();
    }

    @Test
    void stripsCredentialsFromUrls() {
        assertThat(SecretMasker.maskUrlCredentials("mongodb://user:pass@host:27017/db"))
                .isEqualTo("mongodb://" + SecretMasker.MASK + "@host:27017/db");
        assertThat(SecretMasker.maskUrlCredentials("redis://localhost:6379")).isEqualTo("redis://localhost:6379");
        // An '@' in the path must not be treated as user-info.
        assertThat(SecretMasker.maskUrlCredentials("http://host/path@thing")).isEqualTo("http://host/path@thing");
        assertThat(SecretMasker.maskUrlCredentials("not-a-url")).isEqualTo("not-a-url");
        assertThat(SecretMasker.maskUrlCredentials(null)).isNull();
    }
}
