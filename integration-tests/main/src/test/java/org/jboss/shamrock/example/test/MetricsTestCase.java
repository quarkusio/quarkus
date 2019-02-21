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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

@ShamrockTest
public class MetricsTestCase {


    @Test
    public void testMetrics() {
        testCounted("0.0");
        invokeResource();
        testCounted("1.0");
    }

    @Test
    public void testScopes() {
        RestAssured.when().get("/metrics/base").then().statusCode(200);
        RestAssured.when().get("/metrics/vendor").then().statusCode(200);
        RestAssured.when().get("/metrics/application").then().statusCode(200);
        RestAssured.when().get("/metrics/vendor/memory.heap.usage").then().statusCode(200);
    }

    @Test
    public void testInvalidScopes() {
        RestAssured.when().get("/metrics/foo").then().statusCode(404)
                .body(containsString("Bad scope requested"));
        RestAssured.when().get("/metrics/vendor/foo").then().statusCode(404)
                .body(containsString("Metric vendor/foo not found"));
    }

    private void testCounted(String val) {
        RestAssured.when().get("/metrics").then()
                .body(containsString("application:org_jboss_shamrock_example_metrics_metrics_resource_a_counted_resource " + val));
    }

    public void invokeResource() {
        RestAssured.when().get("/metricsresource").then()
                .body(is("TEST"));
    }

}
