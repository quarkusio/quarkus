package org.jboss.shamrock.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonObject;

import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class JaxRSTestCase {

    @Test
    public void testJAXRS() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/test");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("TEST", new String(out.toByteArray()));
    }

    @Test
    public void testJsonp() throws Exception {

        URL uri = new URL("http://localhost:8080/rest/test/jsonp");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        JsonObject obj = Json.createReader(new ByteArrayInputStream(out.toByteArray())).readObject();
        Assert.assertEquals("Stuart", obj.getString("name"));
        Assert.assertEquals("A Value", obj.getString("value"));
    }

}
