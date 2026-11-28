package io.quarkus.spiffe.client.deployment.test;

import static org.hamcrest.Matchers.is;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(SpiffeX509TestResource.class)
public class SpiffeX509SvidDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root.addClass(MutualTlsRoutes.class)
                    .addAsResource(new StringAsset("""
                            quarkus.spiffe-client.endpoint-socket=${x509.quarkus.spiffe-client.endpoint-socket}
                            quarkus.http.tls-configuration-name=spiffe-server
                            quarkus.http.ssl.client-auth=required
                            quarkus.http.insecure-requests=enabled
                            """), "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-tls-registry-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-vertx-http-deployment", Version.getVersion())));

    @Test
    void mutualTlsWorksBeforeAndAfterLiveReload() {
        RestAssured.get("/call").then().statusCode(200).body(is("before " + SpiffeX509TestResource.DEFAULT_SPIFFE_ID));
        TEST.modifySourceFile(MutualTlsRoutes.class, source -> source.replace("\"before \"", "\"after \""));
        RestAssured.get("/call").then().statusCode(200).body(is("after " + SpiffeX509TestResource.DEFAULT_SPIFFE_ID));
    }
}
