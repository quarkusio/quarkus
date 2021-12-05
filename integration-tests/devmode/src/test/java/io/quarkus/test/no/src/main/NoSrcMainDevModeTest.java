package io.quarkus.test.no.src.main;

import static org.hamcrest.Matchers.is;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class NoSrcMainDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(NoSrcMainResource.class)
                    .addAsResource(new StringAsset("test.message = Hello from NoSrcMainDevModeTest"),
                            "application.properties"));

    @Test
    public void validateConfigBean() {
        Assertions.assertFalse(Files.exists(Paths.get("src/main")), "Non-existence of src/main is a prerequisite of this test");
        RestAssured.get("/message").then().body(is("Hello from NoSrcMainDevModeTest"));
        TEST.modifySourceFile(NoSrcMainResource.class,
                oldSource -> oldSource.replace("return message;", "return \"Changed on the fly!\";"));
        RestAssured.get("/message").then().body(is("Changed on the fly!"));
    }
}
