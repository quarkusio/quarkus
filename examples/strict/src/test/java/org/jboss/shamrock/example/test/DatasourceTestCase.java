package org.jboss.shamrock.example.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hsqldb.Server;
import org.hsqldb.server.WebServer;
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
    public void testDataSource() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/datasource");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("10", new String(out.toByteArray()));
    }

    @Test
    public void testDataSourceTransactions() throws Exception {
        URL uri = new URL("http://localhost:8080/rest/datasource/txn");
        URLConnection connection = uri.openConnection();
        InputStream in = connection.getInputStream();
        byte[] buf = new byte[100];
        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        Assert.assertEquals("PASSED", new String(out.toByteArray()));
    }
}
