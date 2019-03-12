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

package io.quarkus.undertow.test.sessioncontext;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

public class SessionContextTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(TestServlet.class, Foo.class));

    @Test
    public void testServlet() {
        Response response = when().get("/foo");
        String sessionId = response.sessionId();
        response.then().statusCode(200).body(is("count=1"));
        given().sessionId(sessionId).when().get("/foo").then().statusCode(200).body(is("count=2"));
        // Destroy session
        when().get("/foo?destroy=true").then().statusCode(200);
        response = when().get("/foo");
        assertNotEquals(sessionId, response.sessionId());
        response.then().statusCode(200).body(is("count=1"));
    }

}
