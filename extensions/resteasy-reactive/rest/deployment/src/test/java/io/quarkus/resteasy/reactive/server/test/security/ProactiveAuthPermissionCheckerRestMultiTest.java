package io.quarkus.resteasy.reactive.server.test.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class ProactiveAuthPermissionCheckerRestMultiTest extends AbstractPermissionCheckerRestMultiTest {

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class, TestIdentityController.class,
                    TestIdentityProvider.class));

}
