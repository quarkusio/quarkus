package io.quarkus.gcp.function.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import com.google.cloud.functions.invoker.runner.Invoker;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CloudEventStorageTestCase.CloudEventFunctionTestProfile.class)
class CloudEventStorageTestCase {
    @Test
    public void test() throws Exception {
        // start the invoker without joining to avoid blocking the thread
        Invoker invoker = new Invoker(
                8081,
                "io.quarkus.gcp.functions.QuarkusCloudEventsFunction",
                "cloudevent",
                Thread.currentThread().getContextClassLoader());
        invoker.startTestServer();

        // test the function using RestAssured
        given()
                .header("ce-specversion", "1.0")
                .header("ce-id", "1234567890")
                .header("ce-type", "google.cloud.storage.object.v1.finalized")
                .header("ce-source", "//storage.googleapis.com/projects/_/buckets/MY-BUCKET-NAME")
                .header("ce-subject", "objects/MY_FILE.txt")
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
                .when()
                .post("http://localhost:8081")
                .then()
                .statusCode(200);

        // stop the invoker
        invoker.stopServer();
    }

    public static class CloudEventFunctionTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "cloud-event";
        }
    }
}
