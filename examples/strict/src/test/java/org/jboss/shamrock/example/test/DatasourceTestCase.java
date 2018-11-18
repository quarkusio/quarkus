package org.jboss.shamrock.example.test;

import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class DatasourceTestCase {


    @Test
    public void testDataSource() {
        Assert.assertEquals("10", URLTester.relative("rest/datasource").invokeURL().asString());
    }

    @Test
    public void testDataSourceTransactions() {
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txn").invokeURL().asString());
    }

}
