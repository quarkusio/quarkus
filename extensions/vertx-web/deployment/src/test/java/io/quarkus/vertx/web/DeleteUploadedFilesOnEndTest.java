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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class DeleteUploadedFilesOnEndTest {
    private static final String UPLOADS_DIR = "target/delete-uploaded-files-on-end-" + UUID.randomUUID().toString();
    private static final byte[] CAFEBABE_BYTES = new byte[] { 0xc, 0xa, 0xf, 0xe, 0xb, 0xa, 0xb, 0xe };
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClasses(Routes.class)
            .addAsResource(new StringAsset("quarkus.http.body.delete-uploaded-files-on-end = true\n" //
                    + "quarkus.http.body.handle-file-uploads = true\n" //
                    + "quarkus.http.body.uploads-directory = " + UPLOADS_DIR + "\n"), "application.properties"));

    @Test
    public void upload() throws IOException {

        final String cafeBabe = "cafe babe";
        final String uploadedPath = RestAssured.given().contentType("multipart/form-data")
                .multiPart("file", "bytes.bin", CAFEBABE_BYTES).formParam("description", cafeBabe)
                .formParam("echoAttachment", "bytes.bin").post("/vertx-web/upload") //
                .then().statusCode(200).extract().body().asString();
        Assertions.assertFalse(uploadedPath.trim().isEmpty());
        /* Wait up to 5 seconds for the file to disappear */
        final long deadline = System.currentTimeMillis() + 5000;
        while (Files.exists(Paths.get(uploadedPath)) && System.currentTimeMillis() < deadline) {
        }
        Assertions.assertFalse(Files.exists(Paths.get(uploadedPath)));
    }

    public static class Routes {
        @Route(path = "/vertx-web/upload", methods = HttpMethod.POST)
        void upload(RoutingContext context) throws IOException {
            final HttpServerResponse response = context.response();
            response.headers().set("Content-Type", "text/plain");
            response.setChunked(true);
            if (context.fileUploads().isEmpty()) {
                response.setStatusCode(500);
                response.end("context.fileUploads() should not be empty");
            }

            for (FileUpload upload : context.fileUploads()) {
                final Path path = Paths.get(upload.uploadedFileName());
                final byte[] actualBytes = Files.readAllBytes(path);
                if (!Arrays.equals(actualBytes, CAFEBABE_BYTES)) {
                    response.setStatusCode(500);
                    response.end("Unexpected content in " + upload.uploadedFileName());
                    return;
                }
                response.setStatusCode(200);
                response.write(upload.uploadedFileName());
            }
            response.end();
        }
    }
}
