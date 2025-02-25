package io.quarkus.resteasy.reactive.server.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class LazyAuthPermissionCheckerRestMultiTest extends AbstractPermissionCheckerRestMultiTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.proactive=false
                            """), "application.properties")
                    .addClasses(TestResource.class, TestIdentityController.class, TestIdentityProvider.class));

}
