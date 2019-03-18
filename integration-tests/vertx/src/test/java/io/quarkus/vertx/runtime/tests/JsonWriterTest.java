/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.vertx.runtime.tests;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Thomas Segismont
 */
@QuarkusTest
public class JsonWriterTest {

    @Test
    public void testJsonSync() {
        RestAssured.when().get("/vertx-test/body-writers/json/sync").then()
                .statusCode(200).body("Hello", equalTo("World"));
    }

    @Test
    public void testArraySync() {
        RestAssured.when().get("/vertx-test/body-writers/array/sync").then()
                .statusCode(200).body("", equalTo(Arrays.asList("Hello", "World")));
    }

    @Test
    public void testJsonAsync() {
        RestAssured.when().get("/vertx-test/body-writers/json/async").then()
                .statusCode(200).body("Hello", equalTo("World"));
    }

    @Test
    public void testArrayAsync() {
        RestAssured.when().get("/vertx-test/body-writers/array/async").then()
                .statusCode(200).body("", equalTo(Arrays.asList("Hello", "World")));
    }
}
