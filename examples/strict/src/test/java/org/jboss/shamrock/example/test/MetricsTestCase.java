package org.jboss.shamrock.example.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class MetricsTestCase {


    @Test
    public void testMetrics() throws Exception {
        testCounted("0.0");
        invokeResource();
        testCounted("1.0");
    }

    private void testCounted(String val) throws IOException {
        URL uri = new URL("http://localhost:8080/metrics");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        String output = new String(out.toByteArray());
        Assert.assertTrue(output, output.contains("application:org_jboss_shamrock_example_metrics_metrics_resource_a_counted_resource " + val));
    }

    public void invokeResource() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/metrics");
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
}
