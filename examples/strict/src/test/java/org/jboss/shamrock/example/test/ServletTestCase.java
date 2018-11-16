package org.jboss.shamrock.example.test;

import java.io.IOException;

import org.jboss.shamrock.example.testutils.URLResponse;
import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class ServletTestCase {

    @Test
    public void testServlet() {
        Assert.assertEquals("A message", URLTester.relative("test").invokeURL().asString());
    }

    @Test
    public void testFilter() {
        Assert.assertEquals("A Filter", URLTester.relative("filter").invokeURL().asString());
    }

    @Test
    public void testStaticResource() {
        Assert.assertTrue(URLTester.relative("index.html").invokeURL().asString().contains("A HTML page"));
    }

    @Test
    public void testWelcomeFile() {
        Assert.assertTrue(URLTester.relative("/").invokeURL().asString().contains("A HTML page"));
    }

    // Basic @ServletSecurity test
    @Test()
    public void testSecureAccessFailure() {
        URLResponse response = URLTester.relative("secure-test").invokeURL();
        Assert.assertEquals(403, response.statusCode());
        Assert.assertTrue(response.exception() != null);
    }
}
