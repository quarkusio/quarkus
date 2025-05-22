package io.quarkus.stork;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class StorkNetworkInterfaceWrapper implements NetworkInterfaceWrapper {
    private final NetworkInterface networkInterface;

    public StorkNetworkInterfaceWrapper(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    @Override
    public boolean isUp() throws SocketException {
        return networkInterface.isUp();
    }

    @Override
    public boolean isLoopback() throws SocketException {
        return networkInterface.isLoopback();
    }

    @Override
    public Enumeration<InetAddress> getInetAddresses() {
        return networkInterface.getInetAddresses();
    }
}
