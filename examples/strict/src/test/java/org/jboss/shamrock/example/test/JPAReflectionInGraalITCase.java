package org.jboss.shamrock.example.test;

import org.jboss.shamrock.junit.GraalTest;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@RunWith(GraalTest.class)
public class JPAReflectionInGraalITCase {

    @Test
    public void testFieldAndGetterReflectionOnEntityFromServlet() throws Exception {
        URL uri = new URL("http://localhost:8080/jpa/test");
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
