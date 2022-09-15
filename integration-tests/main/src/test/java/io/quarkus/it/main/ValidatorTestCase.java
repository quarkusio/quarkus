package io.quarkus.it.main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import jakarta.json.Json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTestExtension;

public class ValidatorTestCase {

    @RegisterExtension
    static QuarkusTestExtension quarkusTestExtension = new QuarkusTestExtension();

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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = connection.getInputStream()) {
            byte[] buf = new byte[100];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
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
