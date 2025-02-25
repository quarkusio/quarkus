package io.quarkus.oidc.redis.token.state.manager.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DefaultDsRedisTokenStateManagerTest extends AbstractRedisTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest();

}
