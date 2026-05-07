package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class PortShadowDetectionTest {

    private static final int TIMEOUT_MS = 500;

    @Test
    void detectShadowOnWildcardIPv4() throws IOException {
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("0.0.0.0", 0));
            int port = server.getLocalPort();

            InetAddress nonLoopback = findNonLoopbackAddress();
            if (nonLoopback == null) {
                return;
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(nonLoopback, port), TIMEOUT_MS);
                assertTrue(socket.isConnected(), "Should detect process on wildcard via non-loopback IP");
            }
        }
    }

    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void detectShadowOnWildcardIPv6() throws IOException {
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("::", 0));
            int port = server.getLocalPort();

            InetAddress nonLoopback = findNonLoopbackAddress();
            if (nonLoopback == null) {
                return;
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(nonLoopback, port), TIMEOUT_MS);
                assertTrue(socket.isConnected(), "Should detect process on IPv6 wildcard via non-loopback IP");
            }
        }
    }

    @Test
    void noFalsePositiveOnLoopbackOnly() throws IOException {
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            int port = server.getLocalPort();

            InetAddress nonLoopback = findNonLoopbackAddress();
            if (nonLoopback == null) {
                return;
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(nonLoopback, port), TIMEOUT_MS);
                fail("Should not connect to loopback-only server via non-loopback IP");
            } catch (IOException expected) {
                // Connection refused — confirms no false positive for loopback-only binding
            }
        }
    }

    private InetAddress findNonLoopbackAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            return null;
        }
        return null;
    }
}
