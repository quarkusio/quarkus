package org.jboss.shamrock.example.test;

import java.io.InputStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.shamrock.example.testutils.URLTester;
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
    public void testJAXRS() {
        Assert.assertEquals("TEST", URLTester.relative("rest/test").invokeURL().asString());
    }

    @Test
    public void testInteger() {
        Assert.assertEquals("11", URLTester.relative("rest/test/int/10").invokeURL().asString());
    }

    @Test
    public void testContextInjection() {
        Assert.assertEquals("/rest/test/request-test", URLTester.relative("rest/test/request-test").invokeURL().asString());
    }

    @Test
    public void testJsonp() {
        JsonObject obj = URLTester.relative("rest/test/jsonp").invokeURL().asJsonReader().readObject();
        Assert.assertEquals("Stuart", obj.getString("name"));
        Assert.assertEquals("A Value", obj.getString("value"));
    }

    @Test
    public void testJackson() {
        JsonObject obj = URLTester.relative("rest/test/jackson").invokeURL().asJsonReader().readObject();
        Assert.assertEquals("Stuart", obj.getString("name"));
        Assert.assertEquals("A Value", obj.getString("value"));
    }

    @Test
    public void testJaxb() throws Exception {
        final InputStream inputStream = URLTester.relative("rest/test/xml").invokeURL().asInputStream();
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(inputStream);
        Element root = dom.getDocumentElement();
        Assert.assertEquals("xmlObject", root.getTagName());
        NodeList value = root.getElementsByTagName("value");
        Assert.assertEquals(1, value.getLength());
        Assert.assertEquals("A Value", value.item(0).getTextContent());
    }

    @Test
    public void testBytecodeTransformation() {
        Assert.assertEquals("Transformed Endpoint", URLTester.relative("rest/test/transformed").invokeURL().asString());
    }

    @Test
    public void testRxJava() {
        Assert.assertEquals("Hello", URLTester.relative("rest/test/rx").invokeURL().asString());
    }

    @Test
    public void testComplexObjectReflectionRegistration() {

        JsonArray obj = URLTester.relative("rest/test/complex").invokeURL().asJsonReader().readArray();
        Assert.assertEquals(1, obj.size());
        JsonObject val = obj.getJsonObject(0);
        System.out.println(val);
        Assert.assertEquals("component value", val.getString("value"));

        JsonArray collection = val.getJsonArray("collectionTypes");
        Assert.assertEquals(1, collection.size());
        Assert.assertEquals("collection type", collection.getJsonObject(0).getString("value"));

        JsonObject sub = val.getJsonObject("subComponent");
        JsonArray subArray = sub.getJsonArray("data");
        Assert.assertEquals(1, subArray.size());
        Assert.assertEquals("sub component list value", subArray.getString(0));

    }


}
