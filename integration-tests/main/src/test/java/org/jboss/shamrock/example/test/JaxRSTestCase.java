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

package org.jboss.shamrock.example.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@ShamrockTest
public class JaxRSTestCase {

    @Test
    public void testJAXRS() {
        RestAssured.when().get("/test").then().body(is("TEST"));
    }

    @Test
    public void testInteger() {
        RestAssured.when().get("/test/int/10").then().body(is("11"));
    }

    @Test
    public void testNonCdiBeansAreApplicationScoped() {
        RestAssured.when().get("/test/count").then().body(is("1"));
        RestAssured.when().get("/test/count").then().body(is("2"));
        RestAssured.when().get("/test/count").then().body(is("3"));
    }

    @Test
    public void testContextInjection() {
        RestAssured.when().get("/test/request-test").then().body(is("/test/request-test"));
    }

    @Test
    public void testJsonp() {
        RestAssured.when().get("/test/jsonp").then()
                .body("name", is("Stuart"),
                        "value", is("A Value"));
    }

    @Test
    public void testJackson() {
        RestAssured.when().get("/test/jackson").then()
                .body("name", is("Stuart"),
                        "value", is("A Value"));
    }

    @Test
    public void testJaxb() throws Exception {
        try {
            // in the native image case, the right parser is not chosen, despite the content-type being correct
            RestAssured.defaultParser = Parser.XML;

            RestAssured.when().get("/test/xml").then()
                    .body("xmlObject.value.text()", is("A Value"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testBytecodeTransformation() {
        RestAssured.when().get("/test/transformed").then().body(is("Transformed Endpoint"));
    }

    @Test
    public void testRxJava() {
        RestAssured.when().get("/test/rx").then().body(is("Hello"));
    }

    @Test
    public void testCustomProvider() {
        RestAssured.when().get("/test/fooprovider").then().body(is("hello-foo"));
    }

    @Test
    public void testComplexObjectReflectionRegistration() {
        RestAssured.when().get("/test/complex").then()
                .body("$.size()", is(1),
                        "[0].value", is("component value"),
                        "[0].collectionTypes.size()", is(1),
                        "[0].collectionTypes[0].value", is("collection type"),
                        "[0].subComponent.data.size()", is(1),
                        "[0].subComponent.data[0]", is("sub component list value"));
    }

    @Test
    public void testSubclass() {
        RestAssured.when().get("/test/subclass").then()
                .body("name", is("my name"),
                        "value", is("my value"));
    }

    @Test
    public void testImplementor() {
        RestAssured.when().get("/test/implementor").then()
                .body("name", is("my name"),
                        "value", is("my value"));
    }

    @Test
    public void testResponse() {
        RestAssured.when().get("/test/response").then()
                .body("name", is("my entity name"),
                        "value", is("my entity value"));
    }
}
