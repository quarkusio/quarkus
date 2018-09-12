package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class MetricsTestCase {


    @Test
    public void testMetrics() {
        testCounted("0.0");
        invokeResource();
        testCounted("1.0");
    }

    private void testCounted(String val) {
        final String metrics = URLTester.relative("metrics").invokeURL().asString();
        Assert.assertTrue(metrics, metrics.contains("application:org_jboss_shamrock_example_metrics_metrics_resource_a_counted_resource " + val));
    }

    public void invokeResource() {
        Assert.assertEquals("TEST", URLTester.relative("rest/metrics").invokeURL().asString());
    }

}
