package io.quarkus.redis.deployment.client.devmode;

import java.util.function.Function;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(RedisTestResource.class)
public class RedisClientDevModeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.redis.hosts=${quarkus.redis.tr}"), "application.properties")
                    .addClass(IncrementResource.class);
        }
    });

    @Test
    public void testSourceFileUpdateInDevMode() {
        RestAssured.get("/inc").then().statusCode(200).body(Matchers.equalTo("1"));

        RestAssured.get("/inc").then().statusCode(200).body(Matchers.equalTo("2"));
        test.modifySourceFile(IncrementResource.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("INCREMENT = 1", "INCREMENT = 10");
            }
        });

        Awaitility.await()
                .untilAsserted(() -> Assertions.assertEquals("2", RestAssured.get("/inc/val").andReturn().asString()));

        RestAssured.get("/inc").then().statusCode(200).body(Matchers.equalTo("12"));
        RestAssured.get("/inc").then().statusCode(200).body(Matchers.equalTo("22"));

        RestAssured.get("/inc").then().statusCode(200).body(Matchers.equalTo("32"));
    }
}
