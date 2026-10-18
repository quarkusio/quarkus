package io.quarkus.redis.deployment.client.devmode;

import java.io.File;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(RedisTestResource.class)
public class RedisClientPreloadDevModeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.vertx.caching=false\n" +
                                            "quarkus.redis.hosts=${quarkus.redis.tr}\n" +
                                            "quarkus.redis.load-only-if-empty=false\n"),
                                    "application.properties")
                            .addAsResource(new File("src/test/resources/imports/starwars.redis"), "import.redis")
                            .addClass(IncrementResource.class);
                }
            });

    @Test
    public void testImportFileUpdateInDevMode() {
        // Verify preloading
        int count = Integer.valueOf(RestAssured.get("/inc/keys")
                .then().statusCode(200).extract().body().asString());

        // Change the import file
        test.modifyResourceFile("import.redis", s -> s + "\nLPUSH list 1 2 3 4 5");
        RestAssured.get("/inc/keys")
                .then().statusCode(200).body(Matchers.equalTo(Integer.toString(count + 1)));
    }
}
