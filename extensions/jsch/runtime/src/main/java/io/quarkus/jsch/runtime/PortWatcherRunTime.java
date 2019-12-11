package io.quarkus.jsch.runtime;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PortWatcherRunTime {

    public static InetAddress anyLocalAddress;

    static {
        try {
            anyLocalAddress = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
        }
    }
}
