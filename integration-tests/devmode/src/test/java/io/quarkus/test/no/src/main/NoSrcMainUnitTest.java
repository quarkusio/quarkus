package io.quarkus.test.no.src.main;

import static org.hamcrest.Matchers.is;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoSrcMainUnitTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(NoSrcMainResource.class)
                    .addAsResource(new StringAsset("test.message = Hello from NoSrcMainUnitTest\n"
                            + "quarkus.oidc.tenant-enabled=false"),
                            "application.properties"));

    @Test
    public void validateConfigBean() {
        Assertions.assertFalse(Files.exists(Paths.get("src/main")), "Non-existence of src/main is a prerequisite of this test");
        RestAssured.get("/message").then().body(is("Hello from NoSrcMainUnitTest"));
    }
}
