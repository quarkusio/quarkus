package io.quarkus.oidc.db.token.state.manager;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MsSqlDbTokenStateManagerTest extends AbstractDbTokenStateManagerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = createQuarkusUnitTest("quarkus-reactive-mssql-client",
            jar -> jar.addAsResource(new StringAsset(System.getProperty("mssql.image")), "container-license-acceptance.txt"));

}
