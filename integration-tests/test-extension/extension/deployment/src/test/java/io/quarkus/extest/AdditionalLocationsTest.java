package io.quarkus.extest;

import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.ext.web.Router;

public class AdditionalLocationsTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> {
                try {
                    String props = new String(FileUtil.readFileContents(
                            AdditionalLocationsTest.class.getClassLoader().getResourceAsStream("application.properties")),
                            StandardCharsets.UTF_8);

                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(DevBean.class)
                            .addAsResource(new StringAsset(props + "\nquarkus.config.locations=additional.properties\n"),
                                    "application.properties")
                            .addAsResource(new StringAsset("additional.property=1234\n"), "additional.properties");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @Test
    void additionalLocations() {
        // A combination of QuarkusUnitTest and QuarkusProdModeTest tests ordering may mess with the port leaving it in
        // 8081 and QuarkusDevModeTest does not changes to the right port.
        RestAssured.port = -1;

        RestAssured.when().get("/config").then()
                .statusCode(200)
                .body(is("1234"));

        TEST.modifyResourceFile("additional.properties", s -> "additional.property=5678\n");

        RestAssured.when().get("/config").then()
                .statusCode(200)
                .body(is("5678"));
    }

    @ApplicationScoped
    public static class DevBean {
        @Inject
        Router router;
        @Inject
        Config config;

        public void register(@Observes StartupEvent ev) {
            router.get("/config").handler(rc -> rc.response().end(config.getValue("additional.property", String.class)));
        }
    }
}
