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

/**
 * Test various Panache operations running in Shamrock
 */
@ShamrockTest
public class PanacheFunctionalityTest {

    @Test
    public void testPanacheFunctionality() throws Exception {
        RestAssured.when().get("/test/model-dao").then().body(is("OK"));
        RestAssured.when().get("/test/model").then().body(is("OK"));
        RestAssured.when().get("/test/rxmodel").then().body(is("OK"));
        RestAssured.when().get("/test/router").then().body(is("OK"));
    }

}
