package io.quarkus.resteasy.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class LazyAuthSecurityEventTest extends AbstractSecurityEventTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TEST_CLASSES)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"), "application.properties"));

}
