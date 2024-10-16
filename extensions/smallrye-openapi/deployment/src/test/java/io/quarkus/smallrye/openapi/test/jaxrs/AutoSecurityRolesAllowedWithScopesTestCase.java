package io.quarkus.smallrye.openapi.test.jaxrs;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class AutoSecurityRolesAllowedWithScopesTestCase extends AutoSecurityRolesAllowedWithScopesTestBase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean.class, OpenApiResourceSecuredAtClassLevel.class,
                            OpenApiResourceSecuredAtMethodLevel.class, OpenApiResourceSecuredAtMethodLevel2.class)
                    .addAsResource(
                            new StringAsset("quarkus.smallrye-openapi.security-scheme=oauth2-implicit\n"
                                    + "quarkus.smallrye-openapi.security-scheme-name=MyScheme\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=Authentication using MyScheme"),
                            "application.properties"));

    @Test
    void testAutoSecurityRequirement() {
        testAutoSecurityRequirement("oauth2");
    }

}
