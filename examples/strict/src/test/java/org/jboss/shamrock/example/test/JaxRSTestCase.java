package org.jboss.shamrock.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    public void testInteger() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/test/int/10");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("11", new String(out.toByteArray()));
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

    @Test
    public void testJackson() throws Exception {

        URL uri = new URL("http://localhost:8080/rest/test/jackson");
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

    @Test
    public void testJaxb() throws Exception {

        URL uri = new URL("http://localhost:8080/rest/test/xml");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(new ByteArrayInputStream(out.toByteArray()));
        Element root = dom.getDocumentElement();
        Assert.assertEquals("xmlObject", root.getTagName());
        NodeList value = root.getElementsByTagName("value");
        Assert.assertEquals(1, value.getLength());
        Assert.assertEquals("A Value", value.item(0).getTextContent());
    }

    @Test
    public void testBytecodeTransformation() throws Exception {

        URL uri = new URL("http://localhost:8080/rest/test/transformed");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("Transformed Endpoint", new String(out.toByteArray()));
    }

    @Test
    public void testRxJava() throws Exception {

        URL uri = new URL("http://localhost:8080/rest/test/rx");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("Hello", new String(out.toByteArray()));
    }
}
