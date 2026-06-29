package io.quarkus.spiffe.client.deployment.test;

import static io.quarkus.spiffe.client.api.JwtSvidRequest.forAudience;
import static io.quarkus.spiffe.client.deployment.SpiffeDevServicesProcessor.DEFAULT_SPIFFE_ID;
import static io.quarkus.spiffe.client.deployment.SpiffeDevServicesProcessor.TEST_TRUST_DOMAIN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spiffe.client.api.JwtSvid;
import io.quarkus.spiffe.client.api.JwtSvidRequest;
import io.quarkus.spiffe.client.api.SpiffeClient;
import io.quarkus.test.QuarkusExtensionTest;

class SpiffeClientConfiguredAudiencesTest {

    private static final String CONFIGURED_AUD_A = TEST_TRUST_DOMAIN + "/configured-a";
    private static final String CONFIGURED_AUD_B = TEST_TRUST_DOMAIN + "/configured-b";
    private static final String REQUEST_AUD = TEST_TRUST_DOMAIN + "/request-override";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.spiffe-client.devservices.transport", "tcp")
            .overrideRuntimeConfigKey("quarkus.spiffe-client.audiences", CONFIGURED_AUD_A + "," + CONFIGURED_AUD_B);

    @Inject
    SpiffeClient spiffeClient;

    @Test
    void noArgFetchUsesConfiguredAudiences() {
        JwtSvid svid = spiffeClient.fetchJwtSvid().await().atMost(TIMEOUT);

        assertThat(svid.spiffeId()).isEqualTo(DEFAULT_SPIFFE_ID);
        assertThat(svid.audience()).containsExactlyInAnyOrder(CONFIGURED_AUD_A, CONFIGURED_AUD_B);
    }

    @Test
    void emptyRequestAudiencesUsesConfiguredAudiences() {
        JwtSvid svid = spiffeClient.fetchJwtSvid(JwtSvidRequest.builder().build()).await().atMost(TIMEOUT);

        assertThat(svid.audience()).containsExactlyInAnyOrder(CONFIGURED_AUD_A, CONFIGURED_AUD_B);
    }

    @Test
    void requestAudiencesOverrideConfiguredOnes() {
        JwtSvid svid = spiffeClient.fetchJwtSvid(forAudience(REQUEST_AUD)).await().atMost(TIMEOUT);

        assertThat(svid.audience()).containsExactly(REQUEST_AUD);
        assertThat(svid.audience()).doesNotContain(CONFIGURED_AUD_A, CONFIGURED_AUD_B);
    }
}
