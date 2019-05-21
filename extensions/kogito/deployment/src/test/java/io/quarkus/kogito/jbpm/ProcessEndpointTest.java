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

package io.quarkus.kogito.jbpm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.kie.kogito.Application;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class ProcessEndpointTest {

    static {
        System.setProperty("resteasy.use.builtin.providers", "true");
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("test-process.bpmn",
                            "src/main/resources/test-process.bpmn")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Inject
    Application application;

    @Test
    public void testProcessRestEndpoint() {

        given()
                .body("{}")
                .contentType(ContentType.JSON)
                .when()
                .post("/tests")
                .then()
                .statusCode(200)
                .body("id", is(1));
    }
}
