package io.quarkus.runtime.ssl;

public class SslContextConfiguration {

    private static boolean sslNativeEnabled;

    public static void setSslNativeEnabled(boolean sslNativeEnabled) {
        SslContextConfiguration.sslNativeEnabled = sslNativeEnabled;
    }

    public static boolean isSslNativeEnabled() {
        return sslNativeEnabled;
    }
}
