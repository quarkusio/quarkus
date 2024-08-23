package io.quarkus.resteasy.reactive.server.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LazyAuthSecurityEventTest extends AbstractSecurityEventTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(TEST_CLASSES)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"), "application.properties"));

}
