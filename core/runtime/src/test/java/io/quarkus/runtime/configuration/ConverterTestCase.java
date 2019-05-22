package io.quarkus.runtime.configuration;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.wildfly.common.net.CidrAddress;
import org.wildfly.common.net.Inet;

/**
 */
public class ConverterTestCase {

    @Test
    public void testSocketAddress() {
        final InetSocketAddressConverter converter = new InetSocketAddressConverter();
        assertEquals(new InetSocketAddress(Inet.parseInet4Address("127.0.0.1"), 1234), converter.convert("127.0.0.1:1234"));
        assertEquals(new InetSocketAddress(Inet.parseInet4Address("127.0.0.1"), 1234), converter.convert("[127.0.0.1]:1234"));
        assertEquals(new InetSocketAddress(Inet.parseInet6Address("::"), 1234), converter.convert("[::]:1234"));
        assertEquals(new InetSocketAddress(Inet.parseInet6Address("::"), 1234), converter.convert("[[::]]:1234"));
        assertEquals(InetSocketAddress.createUnresolved("some-host-name.foo", 1234),
                converter.convert("some-host-name.foo:1234"));
        assertEquals(InetSocketAddress.createUnresolved("some-host-name.foo", 1234),
                converter.convert("[some-host-name.foo]:1234"));
        assertEquals(InetSocketAddress.createUnresolved("some-host-name.foo", 1234),
                converter.convert("[[some-host-name.foo]]:1234"));
    }

    @Test
    public void testInetAddress() {
        final InetAddressConverter converter = new InetAddressConverter();
        assertEquals(Inet.getInet4Address(127, 0, 0, 1), converter.convert("127.0.0.1"));
        assertEquals(Inet.parseInet6Address("ffff::ffff"), converter.convert("ffff::ffff"));
    }

    @Test
    public void testInetAddressResolving() {
        final InetAddressConverter converter = new InetAddressConverter();
        // make sure it's OK to test localhost
        InetAddress localhost;
        try {
            localhost = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new AssumptionViolatedException("No localhost available for resolution", e);
        }
        assertEquals(localhost, converter.convert("localhost"));
    }

    @Test
    public void testCidrAddress() {
        final CidrAddressConverter converter = new CidrAddressConverter();
        assertEquals(CidrAddress.create(Inet.getInet4Address(10, 20, 0, 0), 16), converter.convert("10.20.0.0/16"));
        assertEquals(CidrAddress.create(Inet.parseInet6Address("::ffee:ddcc"), 52), converter.convert("::ffee:ddcc/52"));
    }
}
