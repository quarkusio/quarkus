package io.quarkus.vertx.http.runtime;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TrustedProxyCheckPartConverterTest {

    private static final TrustedProxyCheckPartConverter CONVERTER = new TrustedProxyCheckPartConverter();

    @Test
    public void testCidrIPv4() throws UnknownHostException {
        var part = CONVERTER.convert("10.0.0.0/24");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertEquals(0, part.port);
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("10.0.0.0"), 0));
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("10.0.0.255"), 0));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("100.100.100.100"), 0));
    }

    @Test
    public void testCidrIPv6() throws UnknownHostException {
        var part = CONVERTER.convert("2001:4860:4860::8888/32");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertEquals(0, part.port);
        Assertions
                .assertTrue(part.proxyCheck.test(InetAddress.getByName("2001:4860:4860:0000:0000:0000:0000:8888"), 0));
        Assertions
                .assertTrue(part.proxyCheck.test(InetAddress.getByName("2001:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));
        Assertions
                .assertFalse(part.proxyCheck.test(InetAddress.getByName("2005:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));
    }

    @Test
    public void testIPv4() throws UnknownHostException {
        var part = CONVERTER.convert("10.0.0.0");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertEquals(0, part.port);
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("10.0.0.0"), 0));
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("10.0.0.0"), 99));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("10.0.0.255"), 0));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("100.100.100.100"), 0));
    }

    @Test
    public void testIPv4AndPort() throws UnknownHostException {
        var part = CONVERTER.convert("10.0.0.0:8085");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("10.0.0.0"), 8085));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("10.0.0.0"), 8000));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("10.0.0.255"), 8085));
        Assertions.assertFalse(part.proxyCheck.test(InetAddress.getByName("100.100.100.100"), 8085));
    }

    @Test
    public void testIPv6() throws UnknownHostException {
        var part = CONVERTER.convert("[2001:4860:4860:0000:0000:0000:0000:8888]");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertEquals(0, part.port);
        Assertions
                .assertTrue(part.proxyCheck.test(InetAddress.getByName("2001:4860:4860:0000:0000:0000:0000:8888"), 0));
        Assertions
                .assertFalse(part.proxyCheck.test(InetAddress.getByName("2001:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));
        Assertions
                .assertFalse(part.proxyCheck.test(InetAddress.getByName("2005:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));

        // test short form
        part = CONVERTER.convert("[::]");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertEquals(0, part.port);
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("0:0:0:0:0:0:0:0"), 0));
        Assertions.assertTrue(part.proxyCheck.test(InetAddress.getByName("0:0:0:0:0:0:0:0"), 99));
        Assertions
                .assertFalse(part.proxyCheck.test(InetAddress.getByName("2001:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));
        Assertions
                .assertFalse(part.proxyCheck.test(InetAddress.getByName("2005:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 0));
    }

    @Test
    public void testIPv6AndPort() throws UnknownHostException {
        var part = CONVERTER.convert("[2001:4860:4860:0000:0000:0000:0000:8888]:8085");
        Assertions.assertNull(part.hostName);
        Assertions.assertNotNull(part.proxyCheck);
        Assertions.assertTrue(
                part.proxyCheck.test(InetAddress.getByName("2001:4860:4860:0000:0000:0000:0000:8888"), 8085));
        Assertions.assertFalse(
                part.proxyCheck.test(InetAddress.getByName("2001:4860:4860:0000:0000:0000:0000:8888"), 8000));
        Assertions.assertFalse(
                part.proxyCheck.test(InetAddress.getByName("2001:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 8085));
        Assertions.assertFalse(
                part.proxyCheck.test(InetAddress.getByName("2005:4860:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"), 8085));
    }

    @Test
    public void testHostName() {
        var part = CONVERTER.convert("quarkus.io");
        Assertions.assertNull(part.proxyCheck);
        Assertions.assertNotNull(part.hostName);
        Assertions.assertEquals(0, part.port);
    }

    @Test
    public void testHostNameAndPort() {
        var part = CONVERTER.convert("quarkus.io:8085");
        Assertions.assertNull(part.proxyCheck);
        Assertions.assertNotNull(part.hostName);
        Assertions.assertEquals(part.port, 8085);
    }

}
