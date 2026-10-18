package io.quarkus.restclient.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.eclipse.microprofile.config.Config;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.restclient.NoopHostnameVerifier;

public class QuarkusRestClientBuilderTest {

    private static final String TLS_TRUST_ALL = "quarkus.tls.trust-all";

    @Test
    public void preservesCustomSslContextWhenTrustAllEnabled() throws Exception {
        QuarkusRestClientBuilder builder = new QuarkusRestClientBuilder();

        // set a mocked config that enables trust-all
        Config mockConfig = mock(Config.class);
        when(mockConfig.getOptionalValue(TLS_TRUST_ALL, Boolean.class)).thenReturn(Optional.of(Boolean.TRUE));
        setQuarkusRestClientBuilderField(builder, "config", mockConfig);

        // set a custom SSLContext on the builder
        SSLContext custom = SSLContext.getInstance("TLS");
        custom.init(null, null, new SecureRandom());
        setQuarkusRestClientBuilderField(builder, "sslContext", custom);

        ResteasyClientBuilder clientBuilder = mock(ResteasyClientBuilder.class);

        // invoke private configureTrustAll method
        Method m = QuarkusRestClientBuilder.class.getDeclaredMethod("configureTrustAll", ResteasyClientBuilder.class);
        m.setAccessible(true);
        m.invoke(builder, clientBuilder);

        // hostname verifier should be set to NoopHostnameVerifier
        verify(clientBuilder, times(1)).hostnameVerifier(any(NoopHostnameVerifier.class));
        // but sslContext should NOT be overridden when the user provided one
        verify(clientBuilder, never()).sslContext(any(SSLContext.class));
    }

    @Test
    public void createsTrustAllSslContextWhenNoCustomProvided() throws Exception {
        QuarkusRestClientBuilder builder = new QuarkusRestClientBuilder();

        // set a mocked config that enables trust-all
        Config mockConfig = mock(Config.class);
        when(mockConfig.getOptionalValue(TLS_TRUST_ALL, Boolean.class)).thenReturn(Optional.of(Boolean.TRUE));
        setQuarkusRestClientBuilderField(builder, "config", mockConfig);

        // ensure sslContext field is null (no custom provided)
        setQuarkusRestClientBuilderField(builder, "sslContext", null);

        ResteasyClientBuilder clientBuilder = mock(ResteasyClientBuilder.class);

        // invoke private configureTrustAll method
        Method m = QuarkusRestClientBuilder.class.getDeclaredMethod("configureTrustAll", ResteasyClientBuilder.class);
        m.setAccessible(true);
        m.invoke(builder, clientBuilder);

        // hostname verifier should be set to NoopHostnameVerifier
        verify(clientBuilder, times(1)).hostnameVerifier(any(NoopHostnameVerifier.class));
        // sslContext should be set to a newly created SSLContext
        verify(clientBuilder, times(1)).sslContext(any(SSLContext.class));
    }

    private static void setQuarkusRestClientBuilderField(Object target, String name, Object value) throws Exception {
        Field f = QuarkusRestClientBuilder.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
