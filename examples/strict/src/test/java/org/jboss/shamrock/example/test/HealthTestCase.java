package org.jboss.shamrock.example.test;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class HealthTestCase {

    @Test
    public void testHealthCheck() {
        JsonReader parser = URLTester.relative("health").invokeURL().asJsonReader();
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
