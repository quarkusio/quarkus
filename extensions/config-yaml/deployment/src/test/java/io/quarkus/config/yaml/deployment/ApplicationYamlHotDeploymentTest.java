package io.quarkus.config.yaml.deployment;

import static io.quarkus.config.yaml.runtime.ApplicationYamlProvider.APPLICATION_YAML;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ApplicationYamlHotDeploymentTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(APPLICATION_YAML)
                    .addClass(FooResource.class));

    @Test
    public void testConfigReload() {
        RestAssured.when().get("/foo").then()
                .statusCode(200)
                .body(is("AAAA"));

        test.modifyResourceFile(APPLICATION_YAML, s -> s.replace("AAAA", "BBBB"));

        RestAssured.when().get("/foo").then()
                .statusCode(200)
                .body(is("BBBB"));
    }
}
