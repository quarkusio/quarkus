package org.jboss.shamrock.example.test;

import org.jboss.shamrock.test.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class OpenTracingTestCase {


    @Test
    public void testOpenTracing() {
        invokeResource();
    }

    //private void testCounted(String val) {
        //final String metrics = URLTester.relative("metrics").invokeURL().asString();
        //Assert.assertTrue(metrics, metrics.contains("application:org_jboss_shamrock_example_metrics_metrics_resource_a_counted_resource " + val));
    //}

    public void invokeResource() {
        Assert.assertEquals("TEST", URLTester.relative("rest/opentracing").invokeURL().asString());
    }

}
