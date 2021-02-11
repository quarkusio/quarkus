package io.quarkus.config.yaml.deployment;

import static io.quarkus.config.yaml.runtime.ApplicationYamlProvider.APPLICATION_YAML;
import static io.quarkus.config.yaml.runtime.ApplicationYamlProvider.APPLICATION_YML;
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
                    .addAsResource(APPLICATION_YML)
                    .addClass(FooResource.class));

    @Test
    public void testConfigReload() {
        RestAssured.when().get("/foo").then()
                .statusCode(200)
                .body(is("AAAA"));

        RestAssured.when().get("/foo2").then()
                .statusCode(200)
                .body(is("CCCC"));

        test.modifyResourceFile(APPLICATION_YAML, s -> s.replace("AAAA", "BBBB"));

        RestAssured.when().get("/foo").then()
                .statusCode(200)
                .body(is("BBBB"));

        test.modifyResourceFile(APPLICATION_YML, s -> s.replace("CCCC", "DDDD"));

        RestAssured.when().get("/foo2").then()
                .statusCode(200)
                .body(is("DDDD"));
    }
}
