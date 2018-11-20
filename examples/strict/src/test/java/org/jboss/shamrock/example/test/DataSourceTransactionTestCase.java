package org.jboss.shamrock.example.test;

import org.jboss.shamrock.test.URLResponse;
import org.jboss.shamrock.test.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class DataSourceTransactionTestCase {

    @Test
    public void testTransactionalAnnotation() {
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txninterceptor0").invokeURL().asString());

        URLResponse resp = URLTester.relative("rest/datasource/txninterceptor1").invokeURL();
        Assert.assertTrue(resp.exception() != null);
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txninterceptor2").invokeURL().asString());


    }

}
