package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class TransactionTestCase {

    @Test
    public void testTransaction() {
        Assert.assertEquals("true", URLTester.relative("rest/txn").invokeURL().asString());
    }

}
