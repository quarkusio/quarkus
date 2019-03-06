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

package io.quarkus.example.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.example.rest.ComponentType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class RestClientTestCase {

    @Test
    public void testMicroprofileClient() {
        RestAssured.when().get("/client/manual").then()
                .body(is("TEST"));
    }

    @Test
    public void testMicroprofileClientCDIIntegration() {
        RestAssured.when().get("/client/cdi").then()
                .body(is("TEST"));
    }

    @Test
    void testMicroprofileClientData() {
        JsonPath jsonPath = RestAssured.when().get("/client/manual/jackson").thenReturn().jsonPath();
        Assertions.assertEquals(jsonPath.getString("name"), "Stuart");
        Assertions.assertEquals(jsonPath.getString("value"), "A Value");
    }

    @Test
    void testMicroprofileClientDataCdi() {
        JsonPath jsonPath = RestAssured.when().get("/client/cdi/jackson").thenReturn().jsonPath();
        Assertions.assertEquals(jsonPath.getString("name"), "Stuart");
        Assertions.assertEquals(jsonPath.getString("value"), "A Value");
    }

    @Test
    void testMicroprofileClientComplex() {
        JsonPath jsonPath = RestAssured.when().get("/client/manual/complex").thenReturn().jsonPath();
        List<Map<String, String>> components = jsonPath.getList("$");
        Assertions.assertEquals(components.size(), 1);
        Map<String, String> map = components.get(0);
        Assertions.assertEquals(map.get("value"), "component value");
    }

    @Test
    void testMicroprofileClientComplexCdi() {
        JsonPath jsonPath = RestAssured.when().get("/client/cdi/complex").thenReturn().jsonPath();
        List<Map<String, String>> components = jsonPath.getList("$");
        Assertions.assertEquals(components.size(), 1);
        Map<String, String> map = components.get(0);
        Assertions.assertEquals(map.get("value"), "component value");
    }

    /**
     * Disabled by default as it establishes external connections.
     * <p>
     * Uncomment when you want to test SSL support.
     */
    @Test
    @Disabled
    public void testDegradedSslSupport() {
        RestAssured.when().get("/ssl").then()
                .statusCode(500)
                .body(containsString("SSL support"), containsString("disabled"));
    }
}
