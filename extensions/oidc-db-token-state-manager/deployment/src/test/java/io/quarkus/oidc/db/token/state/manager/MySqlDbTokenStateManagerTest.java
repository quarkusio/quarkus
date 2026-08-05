package io.quarkus.oidc.db.token.state.manager;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

// Becomes flaky in Github CI due to limited resources
@EnabledIfSystemProperty(named = "run-mysql-db-token-state-manager-test", disabledReason = "Insufficient GH CI resources", matches = "true")
public class MySqlDbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = createQuarkusExtensionTest("quarkus-reactive-mysql-client");

}
