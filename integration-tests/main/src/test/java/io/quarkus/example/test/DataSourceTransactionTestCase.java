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

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DataSourceTransactionTestCase {

    @Test
    public void testTransactionalAnnotation() {
        RestAssured.when().get("/datasource/txninterceptor0").then()
                .body(is("PASSED"));

        RestAssured.when().get("/datasource/txninterceptor1").then()
                .statusCode(500);

        RestAssured.when().get("/datasource/txninterceptor2").then()
                .body(is("PASSED"));
    }

}
