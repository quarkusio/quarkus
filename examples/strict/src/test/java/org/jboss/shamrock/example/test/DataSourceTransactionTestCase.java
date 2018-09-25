package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLResponse;
import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class DataSourceTransactionTestCase {

    @Test
    public void testTransactionalAnnotation() {
        //TODO: this does not really belong here, but it saves having to set all the DB stuff up again

        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txninterceptor0").invokeURL().asString());

        try {
            URLResponse resp = URLTester.relative("rest/datasource/txninterceptor1").invokeURL();
            Assert.fail(resp.asString());
        } catch (RuntimeException expected) {

        }
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txninterceptor2").invokeURL().asString());


    }

}
