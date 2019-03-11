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

package io.quarkus.spring.tests;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class InjectedSpringBeansResourceTest {

    @Test
    public void testInjection() {
        RestAssured.when().get("/spring-test").then()
                .body(containsString("YOLO WORLD!"));
    }

    @Test
    public void testRequestScope() {
        RestAssured.when().get("/spring-test/request").then()
                .body(Matchers.is("0"));
        RestAssured.when().get("/spring-test/request").then()
                .body(Matchers.is("1"));
    }

    @Test
    public void testSessionScope() {
        final Response first = when().get("/spring-test/session");
        final String sessionId = first.sessionId();
        first.then()
                .statusCode(200)
                .body(Matchers.is("0"));
        RestAssured.given()
                .sessionId(sessionId)
                .when()
                .get("/spring-test/session")
                .then()
                .statusCode(200)
                .body(Matchers.is("1"));

        RestAssured.given()
                .sessionId(sessionId)
                .when()
                .post("/spring-test/invalidate")
                .then();

        final Response second = RestAssured.given()
                .sessionId(sessionId)
                .when()
                .get("/spring-test/session");
        assertNotEquals(sessionId, second.sessionId());
        second.then()
                .statusCode(200)
                .body(Matchers.is("0"));
    }
}
