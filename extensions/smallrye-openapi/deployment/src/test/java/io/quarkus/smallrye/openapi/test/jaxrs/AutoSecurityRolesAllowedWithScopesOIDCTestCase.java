package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

class AutoSecurityRolesAllowedWithScopesOIDCTestCase extends AutoSecurityRolesAllowedWithScopesTestBase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ResourceBean.class, OpenApiResourceSecuredAtClassLevel.class,
                            OpenApiResourceSecuredAtMethodLevel.class, OpenApiResourceSecuredAtMethodLevel2.class)
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-openapi.security-scheme-name=MyScheme\n"
                                            + "quarkus.smallrye-openapi.security-scheme-description=Authentication using MyScheme\n"
                                            + "quarkus.devservices.enabled=false\n"
                                            + "quarkus.oidc.auth-server-url=http://localhost:8081/auth/realms/OpenAPIOIDC"),
                            "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())));

    @Test
    void testAutoSecurityRequirement() {
        testAutoSecurityRequirement("openIdConnect");
    }
}
