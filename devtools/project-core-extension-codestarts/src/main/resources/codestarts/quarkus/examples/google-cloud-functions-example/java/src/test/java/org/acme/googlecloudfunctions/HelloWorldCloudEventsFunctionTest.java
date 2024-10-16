package org.acme.googlecloudfunctions;

import io.quarkus.google.cloud.functions.test.FunctionType;
import io.quarkus.google.cloud.functions.test.WithFunction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@WithFunction(FunctionType.CLOUD_EVENTS)
class HelloWorldCloudEventsFunctionTest {
    @Test
    void testAccept() {
        given()
                .body("{\n" +
                        "        \"bucket\": \"MY_BUCKET\",\n" +
                        "        \"contentType\": \"text/plain\",\n" +
                        "        \"kind\": \"storage#object\",\n" +
                        "        \"md5Hash\": \"...\",\n" +
                        "        \"metageneration\": \"1\",\n" +
                        "        \"name\": \"MY_FILE.txt\",\n" +
                        "        \"size\": \"352\",\n" +
                        "        \"storageClass\": \"MULTI_REGIONAL\",\n" +
                        "        \"timeCreated\": \"2020-04-23T07:38:57.230Z\",\n" +
                        "        \"timeStorageClassUpdated\": \"2020-04-23T07:38:57.230Z\",\n" +
                        "        \"updated\": \"2020-04-23T07:38:57.230Z\"\n" +
                        "      }")
                .header("ce-specversion", "1.0")
                .header("ce-id", "1234567890")
                .header("ce-type", "google.cloud.storage.object.v1.finalized")
                .header("ce-source", "//storage.googleapis.com/projects/_/buckets/MY-BUCKET-NAME")
                .header("ce-subject", "objects/MY_FILE.txt")
                .when()
                .post()
                .then()
                .statusCode(200);
    }
}