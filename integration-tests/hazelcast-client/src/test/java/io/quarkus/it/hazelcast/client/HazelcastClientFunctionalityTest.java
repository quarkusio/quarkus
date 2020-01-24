package io.quarkus.it.hazelcast.client;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.hazelcast.HazelcastServerTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(HazelcastServerTestResource.class)
public class HazelcastClientFunctionalityTest {

    @Test
    public void shouldReadFromDistributedMap() {
        RestAssured
                .when().get("/hazelcast-client/ds/get?key=nonexisting")
                .then().body(is("default"));
    }

    @Test
    public void shouldWriteDataSerializableToDistributedMap() {
        RestAssured
                .when().post("/hazelcast-client/ds/put?key=foo&value=foo_value")
                .thenReturn();

        RestAssured
                .when().get("/hazelcast-client/ds/get?key=foo")
                .then().body(is("foo_value"));
    }

    @Test
    public void shouldWriteIdentifiedDataSerializableToDistributedMap() {
        RestAssured
                .when().post("/hazelcast-client/ids/put?key=foo&value=foo_value")
                .thenReturn();

        RestAssured
                .when().get("/hazelcast-client/ids/get?key=foo")
                .then().body(is("foo_value"));
    }

    @Test
    public void shouldWritePortableToDistributedMap() {
        RestAssured
                .when().post("/hazelcast-client/ptable/put?key=foo&value=foo_value")
                .thenReturn();

        RestAssured
                .when().get("/hazelcast-client/ptable/get?key=foo")
                .then().body(is("foo_value"));
    }
}
