package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class RequestScopeTestCase {

    @Test
    public void testRequestScope() {
        Assert.assertEquals("3", URLTester.relative("rest/request-scoped").invokeURL().asString());
        Assert.assertEquals("3", URLTester.relative("rest/request-scoped").invokeURL().asString());
    }


}
