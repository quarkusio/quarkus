package org.jboss.shamrock.health.test;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shamrock.test.URLTester;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockUnitTest.class)
public class HealthUnitTest {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(BasicHealthCheck.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testHealth() {
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
