package io.quarkus.oidc.db.token.state.manager;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class PostgresDbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = createQuarkusExtensionTest("quarkus-reactive-pg-client");

}
