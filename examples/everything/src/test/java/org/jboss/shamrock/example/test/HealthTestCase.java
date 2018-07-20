package org.jboss.shamrock.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;

import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class HealthTestCase {

    @Test
    public void testHealthCheck() throws Exception {
        URL uri = new URL("http://localhost:8080/health");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        JsonReader parser = Json.createReader(new ByteArrayInputStream(out.toByteArray()));
        JsonObject obj = parser.readObject();
        System.out.println(obj);
        Assert.assertEquals("UP", obj.getString("outcome"));
        JsonArray list = obj.getJsonArray("checks");
        Assert.assertEquals(1, list.size());
        JsonObject check = list.getJsonObject(0);
        Assert.assertEquals("UP", check.getString("state"));
        Assert.assertEquals("basic", check.getString("name"));
    }
}
