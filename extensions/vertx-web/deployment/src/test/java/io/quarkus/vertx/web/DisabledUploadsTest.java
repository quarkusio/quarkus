/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.vertx.web;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.restassured.RestAssured;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class DisabledUploadsTest {
    private static final String UPLOADS_DIR = "target/disabled-uploads-" + UUID.randomUUID().toString();
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Routes.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.body.handle-file-uploads = false\n" //
                                    + "quarkus.http.body.uploads-directory = " + UPLOADS_DIR + "\n"),
                            "application.properties"));

    @Test
    public void upload() throws IOException {

        final byte[] bytes = new byte[] { 0xc, 0xa, 0xf, 0xe, 0xb, 0xa, 0xb, 0xe };
        final String cafeBabe = "cafe babe";
        final String uploadedPath = RestAssured.given().contentType("multipart/form-data").multiPart("file", "bytes.bin", bytes)
                .formParam("description", cafeBabe).formParam("echoAttachment", "bytes.bin")
                .post("/vertx-web/upload").then().statusCode(200)
                .extract().body().asString();
        Assertions.assertTrue(uploadedPath.isEmpty());
        Assertions.assertFalse(new File(UPLOADS_DIR).exists());
    }

    public static class Routes {
        @Route(path = "/vertx-web/upload", methods = HttpMethod.POST)
        void upload(RoutingContext context) {
            context.response().headers().set("Content-Type", "text/plain");
            context.response().setChunked(true).setStatusCode(200);
            for (FileUpload upload : context.fileUploads()) {
                context.response().write(upload.uploadedFileName());
            }
            context.response().end();
        }
    }
}
