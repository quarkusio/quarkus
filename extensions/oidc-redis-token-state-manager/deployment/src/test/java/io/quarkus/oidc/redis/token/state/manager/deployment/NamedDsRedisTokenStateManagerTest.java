package io.quarkus.oidc.redis.token.state.manager.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NamedDsRedisTokenStateManagerTest extends AbstractRedisTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest(
            "quarkus.oidc.redis-token-state-manager.redis-client-name=named-1",
            "quarkus.redis.devservices.enabled=false",
            "quarkus.redis.named-1.client-name=named-1",
            "test.redis-client-name=named-1",
            "quarkus.redis.named-1.devservices.enabled=true");

}
