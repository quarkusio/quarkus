package io.quarkus.oidc.db.token.state.manager;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

// TODO: this test works and we simply need to run it, however in CI it is going to hit
//   hang detection timeout set by 'quarkus.test.hang-detection-timeout=60', we need to discuss and try
//   to find a way to run it (like allow QuarkusUnitTests to override system property etc.)
//   but it will require separate PR and make changes unrelated to DB Token State Manager
@EnabledIfSystemProperty(named = "run-db2-db-token-state-manager-test", disabledReason = "Db2 is slow to start", matches = "true")
public class Db2DbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest("quarkus-reactive-db2-client",
            jar -> jar.addAsResource(new StringAsset(System.getProperty("db2.image")), "container-license-acceptance.txt"));

}
