package io.quarkus.stork;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Enumeration;

public interface NetworkInterfaceWrapper {
    boolean isUp() throws SocketException;

    boolean isLoopback() throws SocketException;

    Enumeration<InetAddress> getInetAddresses();
}
