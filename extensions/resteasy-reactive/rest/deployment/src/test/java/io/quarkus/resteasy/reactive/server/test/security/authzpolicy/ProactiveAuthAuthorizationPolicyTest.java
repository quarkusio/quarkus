package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ProactiveAuthAuthorizationPolicyTest extends AbstractAuthorizationPolicyTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TEST_CLASSES)
                    .addAsResource(new StringAsset(APPLICATION_PROPERTIES), "application.properties"));

}
