package io.quarkus.devui;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

/**
 * Finds a host name of the machine that resolves to an IPv4 loopback address, as {@code /etc/hosts} usually maps
 * the machine name to one.
 */
final class LoopbackHostnames {

    static String find() {
        for (String candidate : candidates()) {
            if (candidate != null && !candidate.equals("localhost") && resolvesToLoopback(candidate)) {
                return candidate;
            }
        }
        Assumptions.abort("No host name of this machine resolves to a loopback address");
        return null;
    }

    private static List<String> candidates() {
        String local;
        try {
            local = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            local = null;
        }
        return java.util.Arrays.asList(local, "localhost.localdomain");
    }

    private static boolean resolvesToLoopback(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (!(address instanceof Inet4Address) || !address.isLoopbackAddress()) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
