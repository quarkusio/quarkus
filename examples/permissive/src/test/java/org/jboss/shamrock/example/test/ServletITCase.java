package org.jboss.shamrock.example.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.shamrock.junit.GraalTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GraalTest.class)
public class ServletITCase {

    @Test
    public void testServlet() throws Exception {
        URL uri = new URL("http://localhost:8080/test");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("A message", new String(out.toByteArray()));
    }

    @Test
    public void testStaticResource() throws Exception {
        URL uri = new URL("http://localhost:8080/index.html");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertTrue(new String(out.toByteArray()).contains("A HTML page"));
    }
}
