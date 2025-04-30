package io.quarkus.oidc.db.token.state.manager;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PostgresDbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest("quarkus-reactive-pg-client");

}
