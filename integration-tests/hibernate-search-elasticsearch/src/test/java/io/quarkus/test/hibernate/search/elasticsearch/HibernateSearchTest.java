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
public class HibernateSearchTest {

    @Test
    public void testSearch() throws Exception {
        RestAssured.when().put("/test/hibernate-search/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/hibernate-search/search").then()
                .statusCode(200)
                .body(is("OK"));

        RestAssured.when().put("/test/hibernate-search/mass-indexer").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
