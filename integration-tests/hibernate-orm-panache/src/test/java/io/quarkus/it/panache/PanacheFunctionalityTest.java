/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.it.panache;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnSubstrate;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
public class PanacheFunctionalityTest {

    @Test
    public void testPanacheFunctionality() throws Exception {
        RestAssured.when().get("/test/model-dao").then().body(is("OK"));
        RestAssured.when().get("/test/model").then().body(is("OK"));
        RestAssured.when().get("/test/accessors").then().body(is("OK"));

        RestAssured.when().get("/test/model1").then().body(is("OK"));
        RestAssured.when().get("/test/model2").then().body(is("OK"));
        RestAssured.when().get("/test/model3").then().body(is("OK"));
    }

    @Test
    public void testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/test/ignored-properties")
                .then()
                .body(is("{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"));
        RestAssured.given().accept(ContentType.XML)
                .when().get("/test/ignored-properties")
                .then().body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><id>666</id><name>Eddie</name><serialisationTrick>1</serialisationTrick><status>DECEASED</status></person>"));
    }

    @DisabledOnSubstrate
    @Test
    public void testPanacheInTest() {
        Assertions.assertEquals(0, Person.count());
    }
}
