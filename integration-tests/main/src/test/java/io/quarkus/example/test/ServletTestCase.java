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

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ServletTestCase {

    @Test
    public void testServlet() {
        RestAssured.when().get("/testservlet").then()
                .body(is("A message"));
    }

    @Test
    public void testFilter() {
        RestAssured.when().get("/filter").then()
                .body(is("A Filter"));
    }

    @Test
    public void testStaticResource() {
        RestAssured.when().get("/filter").then()
                .body(containsString("A Filter"));
    }

    @Test
    public void testWelcomeFile() {
        RestAssured.when().get("/").then()
                .body(containsString("A HTML page"));
    }

    // Basic @ServletSecurity test
    @Test()
    public void testSecureAccessFailure() {
        RestAssured.when().get("/secure-test").then()
                .statusCode(401);
    }

    // Basic @ServletSecurity test
    @Test()
    public void testSecureAccessSuccess() {
        RestAssured.given().auth().preemptive().basic("stuart", "test")
                .when().get("/secure-test").then()
                .statusCode(200);
    }
}
