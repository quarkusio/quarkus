/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.test.hibernate.search.elasticsearch;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ElasticsearchClientTest {

    @Test
    public void testConnection() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/connection").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testFullCycle() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/full-cycle").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testSniffer() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/sniffer").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
