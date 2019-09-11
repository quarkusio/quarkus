package io.quarkus.yaml.configuration.test;

import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.APPLICATION_YML_FILE;
import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ApplicationYmlHotReloadTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(HotReloadResource.class)
                    .addAsResource("hot-reload-test.yml", APPLICATION_YML_FILE));

    @Test
    public void testHotReload() {
        checkResponse("foo");
        TEST.modifyResourceFile(APPLICATION_YML_FILE, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("foo", "bar");
            }
        });
        checkResponse("bar");
    }

    private void checkResponse(String expectedValue) {
        RestAssured.when().get(HotReloadResource.PATH).then().statusCode(200).body(is(expectedValue));
    }
}
