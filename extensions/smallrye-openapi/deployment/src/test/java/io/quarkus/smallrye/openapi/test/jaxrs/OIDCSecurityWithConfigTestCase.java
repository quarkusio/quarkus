package io.quarkus.smallrye.openapi.test.jaxrs;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class OIDCSecurityWithConfigTestCase extends OIDCSecurityTestBase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=oidc\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=OIDCCompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=OIDC Authentication\n"
                                    + "quarkus.smallrye-openapi.oidc-open-id-connect-url=http://localhost:8081/auth/realms/OpenAPIOIDC/.well-known/openid-configuration"),
                            "application.properties"));
}
