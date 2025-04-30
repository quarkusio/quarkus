package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

class OIDCSecurityAutoAddTestTest extends OIDCSecurityTestBase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class)
                    .addAsResource(
                            new StringAsset(""
                                    + "quarkus.smallrye-openapi.security-scheme-name=OIDCCompanyAuthentication\n"
                                    + "quarkus.smallrye-openapi.security-scheme-description=OIDC Authentication\n"
                                    + "quarkus.smallrye-openapi.oidc-open-id-connect-url=BUILD-TIME-OVERRIDDEN\n"
                                    + "quarkus.http.auth.permission.\"oidc\".policy=authenticated\n"
                                    + "quarkus.http.auth.permission.\"oidc\".paths=/resource/*\n"
                                    + "quarkus.oidc.auth-server-url=BUILD-TIME-OVERRIDDEN\n"
                                    + "quarkus.devservices.enabled=false"),
                            "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())))
            .overrideRuntimeConfigKey("quarkus.oidc.auth-server-url", "http://localhost:8081/auth/realms/OpenAPIOIDC");
}
