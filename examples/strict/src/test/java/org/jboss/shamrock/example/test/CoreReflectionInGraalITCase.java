package org.jboss.shamrock.example.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.shamrock.junit.GraalTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GraalTest.class)
public class CoreReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        URL uri = new URL("http://localhost:8080/core/reflection");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        assertEquals("OK", new String(out.toByteArray()));
    }

}
