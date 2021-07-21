package io.quarkus.mailer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class KeyStoreSSLSocketFactory extends SSLSocketFactory {

    private static final String KEY_STORE_FILE = "src/test/resources/certs/server2.jks";
    private static final String KEY_STORE_PASSWORD = "password";

    private final SSLSocketFactory delegate;

    public KeyStoreSSLSocketFactory() throws GeneralSecurityException, IOException {
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(KEY_STORE_FILE), null);
        keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), null, null);
        delegate = context.getSocketFactory();
    }

    @Override
    public Socket createSocket(final Socket s, final String host, final int port, final boolean autoClose)
            throws IOException {
        return delegate.createSocket(s, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final String arg0, final int arg1) throws IOException, UnknownHostException {
        return delegate.createSocket(arg0, arg1);
    }

    @Override
    public Socket createSocket(final InetAddress arg0, final int arg1) throws IOException {
        return delegate.createSocket(arg0, arg1);
    }

    @Override
    public Socket createSocket(final String arg0, final int arg1, final InetAddress arg2, final int arg3)
            throws IOException, UnknownHostException {
        return delegate.createSocket(arg0, arg1, arg2, arg3);
    }

    @Override
    public Socket createSocket(final InetAddress arg0, final int arg1, final InetAddress arg2, final int arg3)
            throws IOException {
        return delegate.createSocket(arg0, arg1, arg2, arg3);
    }

}
