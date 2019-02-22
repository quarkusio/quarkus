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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.json.Json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ValidatorTestCase {

    @TestHTTPResource("validator/manual")
    URL uri;

    @Test
    public void testManualValidationFailed() throws Exception {
        URLConnection connection = uri.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        byte[] body = Json.createObjectBuilder()
                .add("name", "Stuart")
                .add("email", "aa")
                .build().toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream o = connection.getOutputStream()) {
            o.write(body);
        }

        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assertions.assertEquals("failed:email", new String(out.toByteArray(), "UTF-8"));
    }

    @Test
    public void testManualValidationPassed() throws Exception {
        URLConnection connection = uri.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        byte[] body = Json.createObjectBuilder()
                .add("name", "Stuart")
                .add("email", "test@test.com")
                .build().toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream o = connection.getOutputStream()) {
            o.write(body);
        }

        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assertions.assertEquals("passed", new String(out.toByteArray(), "UTF-8"));
    }

}
