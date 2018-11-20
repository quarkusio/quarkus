package org.jboss.shamrock.example.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.json.Json;

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class ValidatorTestCase {

    @Test
    public void testManualValidationFailed() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/validator/manual");
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
        Assert.assertEquals("failed:email", new String(out.toByteArray()));
    }


    @Test
    public void testManualValidationPassed() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/validator/manual");
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
        Assert.assertEquals("passed", new String(out.toByteArray()));
    }

}
