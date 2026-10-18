package io.quarkus.resteasy.reactive.server.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class LazyAuthFailureResponseBodyDevModeTest extends AbstractAuthFailureResponseBodyDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class, FailingAuthenticator.class, AuthFailure.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.auth.proactive=false
                            """), "application.properties"));

}
