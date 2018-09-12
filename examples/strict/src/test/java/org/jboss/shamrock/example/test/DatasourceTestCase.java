package org.jboss.shamrock.example.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hsqldb.Server;
import org.jboss.shamrock.example.testutils.URLTester;
import org.jboss.shamrock.junit.ShamrockTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockTest.class)
public class DatasourceTestCase {

    static Server ws;

    @BeforeClass
    public static void setup() throws Exception {
        Path dir = Files.createTempDirectory("shamrock-test");
        ws = new Server();
        ws.setPort(7676);
        ws.setAddress("localhost");
        ws.setDatabaseName(0, "test");
        ws.setDatabasePath(0, dir.toAbsolutePath().toString() + File.separator + "tempdb");
        ws.start();
    }

    @AfterClass
    public static void tearDown() {
        ws.stop();
    }

    @Test
    public void testDataSource() {
        Assert.assertEquals("10", URLTester.relative("rest/datasource").invokeURL().asString());
    }

    @Test
    public void testDataSourceTransactions() {
        Assert.assertEquals("PASSED", URLTester.relative("rest/datasource/txn").invokeURL().asString());
    }
}
