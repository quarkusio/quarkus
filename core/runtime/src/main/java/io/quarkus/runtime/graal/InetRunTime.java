package io.quarkus.runtime.graal;

import java.net.Inet4Address;
import java.net.Inet6Address;

import org.wildfly.common.net.Inet;

public class InetRunTime {
    public static final Inet4Address INET4_ANY = Inet.getInet4Address(0, 0, 0, 0);
    public static final Inet4Address INET4_LOOPBACK = Inet.getInet4Address(127, 0, 0, 1);
    public static final Inet4Address INET4_BROADCAST = Inet.getInet4Address(255, 255, 255, 255);
    public static final Inet6Address INET6_ANY = Inet.getInet6Address(0, 0, 0, 0, 0, 0, 0, 0);
    public static final Inet6Address INET6_LOOPBACK = Inet.getInet6Address(0, 0, 0, 0, 0, 0, 0, 1);
}
