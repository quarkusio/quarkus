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

package io.quarkus.it.vertx;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class JsonReaderTest {

    @Test
    public void testJson() {
        String body = new JsonObject().put("Hello", "World").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/vertx-test/json-bodies/json/sync")
                .then().statusCode(200).body(equalTo("Hello World"));
    }

    @Test
    public void testEmptyJson() {
        given().contentType(ContentType.JSON).body("")
                .post("/vertx-test/json-bodies/json/sync")
                .then().statusCode(400);
    }

    @Test
    public void testArray() {
        String body = new JsonArray().add("Hello").add("World").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/vertx-test/json-bodies/array/sync")
                .then().statusCode(200).body(equalTo("Hello World"));
    }

    @Test
    public void testEmptyArray() {
        given().contentType(ContentType.JSON).body("")
                .post("/vertx-test/json-bodies/array/sync")
                .then().statusCode(400);
    }
}
