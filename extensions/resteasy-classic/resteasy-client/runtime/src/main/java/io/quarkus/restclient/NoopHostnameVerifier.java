package io.quarkus.restclient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * The {@link NoopHostnameVerifier} essentially turns hostname verification off.
 */
public class NoopHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }

    @Override
    public final String toString() {
        return "NO_OP";
    }
}
