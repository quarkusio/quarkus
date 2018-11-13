package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class ServletTestCase {

    @Test
    public void testServlet() {
        Assert.assertEquals("hello world", URLTester.relative("test").invokeURL().asString());
    }

}
