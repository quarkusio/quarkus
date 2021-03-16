package io.quarkus.grpc.health;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import grpc.health.v1.HealthGrpc;
import io.quarkus.test.QuarkusUnitTest;

public class MicroProfileHealthDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HealthGrpc.class.getPackage()))
            .withConfigurationResource("no-mp-health-config.properties");

    @Test
    public void shouldNotGetMpHealthInfoWhenDisabled() {
        // @formatter:off
        when()
                .get("/q/health")
        .then()
                .statusCode(200)
                .body("checks.size()", Matchers.equalTo(0));
        // @formatter:on
    }
}
