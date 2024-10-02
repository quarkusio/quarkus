package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.DisableIfBuiltWithGraalVMNewerThan;
import io.quarkus.test.junit.GraalVMVersion;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@QuarkusTestResource(KafkaKeycloakTestResource.class)
public class KafkaOauthTest {

    protected static final TypeRef<List<String>> TYPE_REF = new TypeRef<List<String>>() {
    };

    @Test
    @DisableIfBuiltWithGraalVMNewerThan(GraalVMVersion.GRAALVM_24_0_999) // See https://github.com/quarkusio/quarkus/issues/39634
    public void test() {
        await().untilAsserted(() -> Assertions.assertEquals(2, get("/kafka").as(TYPE_REF).size()));
    }
}
