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
package io.quarkus.gradle.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

/**
 * Integration Test which runs the sample app built by Gradle.
 *
 * @author <a href="mailto:mike@vorburger.ch">Michael Vorburger.ch</a>
 */
public class GradleRunIntegrationTest {
    // do not rename this to *IT because we want it to run on regular mvn test without requiring failsafe

    @Test
    public void testHelloEndpoint() throws Exception {
        try (QuarkusJavaLauncher process = new QuarkusJavaLauncher("../gradle-it/build/gradle-it-runner.jar")) {
            given()
                    .when().get("/hello")
                    .then()
                    .statusCode(200)
                    .body(is("hello"));
        }
    }
}
