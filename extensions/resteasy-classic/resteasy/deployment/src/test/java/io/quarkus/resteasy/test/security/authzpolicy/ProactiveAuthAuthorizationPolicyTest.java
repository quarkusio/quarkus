package io.quarkus.resteasy.test.security.authzpolicy;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ProactiveAuthAuthorizationPolicyTest extends AbstractAuthorizationPolicyTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TEST_CLASSES)
                    .addAsResource(new StringAsset(APPLICATION_PROPERTIES), "application.properties"));

}
