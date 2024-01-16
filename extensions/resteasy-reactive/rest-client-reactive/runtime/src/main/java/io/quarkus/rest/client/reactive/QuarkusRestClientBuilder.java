package io.quarkus.rest.client.reactive;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.core.Configurable;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;

import io.quarkus.rest.client.reactive.runtime.QuarkusRestClientBuilderImpl;
import io.quarkus.rest.client.reactive.runtime.RestClientBuilderImpl;
import io.vertx.core.http.HttpClientOptions;

/**
 * This is the main entry point for creating a Type Safe Quarkus Rest Client.
 * <p>
 * Invoking {@link #newBuilder()} is intended to always create a new instance, not use a cached version.
 * </p>
 * <p>
 * The <code>QuarkusRestClientBuilder</code> is based on {@link RestClientBuilder} class but Quarkus specific.
 * </p>
 */
public interface QuarkusRestClientBuilder extends Configurable<QuarkusRestClientBuilder> {

    static QuarkusRestClientBuilder newBuilder() {
        RestClientBuilderImpl proxy = new RestClientBuilderImpl();
        for (RestClientBuilderListener listener : ServiceLoader.load(RestClientBuilderListener.class)) {
            listener.onNewBuilder(proxy);
        }

        return new QuarkusRestClientBuilderImpl(proxy);
    }

    /**
     * Specifies the base URL to be used when making requests. Assuming that the interface has a
     * <code>@Path("/api")</code> at the interface level and a <code>url</code> is given with
     * <code>http://my-service:8080/service</code> then all REST calls will be invoked with a <code>url</code> of
     * <code>http://my-service:8080/service/api</code> in addition to any <code>@Path</code> annotations included on the
     * method.
     *
     * Subsequent calls to this method will replace the previously specified baseUri/baseUrl.
     *
     * @param url the base Url for the service.
     * @return the current builder with the baseUrl set.
     */
    QuarkusRestClientBuilder baseUrl(URL url);

    /**
     * Specifies the base URI to be used when making requests. Assuming that the interface has a
     * <code>@Path("/api")</code> at the interface level and a <code>uri</code> is given with
     * <code>http://my-service:8080/service</code> then all REST calls will be invoked with a <code>uri</code> of
     * <code>http://my-service:8080/service/api</code> in addition to any <code>@Path</code> annotations included on the
     * method.
     *
     * Subsequent calls to this method will replace the previously specified baseUri/baseUrl.
     *
     * @param uri the base URI for the service.
     * @return the current builder with the baseUri set
     * @throws IllegalArgumentException if the passed in URI is invalid
     */
    QuarkusRestClientBuilder baseUri(URI uri);

    /**
     * Set the connect timeout.
     * <p>
     * Like JAX-RS's <code>jakarta.ws.rs.client.ClientBuilder</code>'s <code>connectTimeout</code> method, specifying a
     * timeout of 0 represents infinity, and negative values are not allowed.
     * </p>
     * <p>
     * If the client instance is injected via CDI and the
     * &quot;<em>fully.qualified.InterfaceName</em>/mp-rest/connectTimeout&quot; property is set via MicroProfile
     * Config, that property's value will override, the value specified to this method.
     * </p>
     *
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument.
     * @return the current builder with the connect timeout set.
     * @throws IllegalArgumentException if the value of timeout is negative.
     */
    QuarkusRestClientBuilder connectTimeout(long timeout, TimeUnit unit);

    /**
     * Set the read timeout.
     * <p>
     * Like JAX-RS's <code>jakarta.ws.rs.client.ClientBuilder</code>'s <code>readTimeout</code> method, specifying a
     * timeout of 0 represents infinity, and negative values are not allowed.
     * </p>
     * <p>
     * Also like the JAX-RS Client API, if the read timeout is reached, the client interface method will throw a
     * <code>jakarta.ws.rs.ProcessingException</code>.
     * </p>
     * <p>
     * If the client instance is injected via CDI and the
     * &quot;<em>fully.qualified.InterfaceName</em>/mp-rest/readTimeout&quot; property is set via MicroProfile Config,
     * that property's value will override, the value specified to this method.
     * </p>
     *
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument.
     * @return the current builder with the connect timeout set.
     * @throws IllegalArgumentException if the value of timeout is negative.
     */
    QuarkusRestClientBuilder readTimeout(long timeout, TimeUnit unit);

    /**
     * Specifies the SSL context to use when creating secured transport connections to server endpoints from web targets
     * created by the client instance that is using this SSL context.
     *
     * @param sslContext the ssl context
     * @return the current builder with ssl context set
     * @throws NullPointerException if the <code>sslContext</code> parameter is null.
     */
    QuarkusRestClientBuilder sslContext(SSLContext sslContext);

    /**
     * Set whether hostname verification is enabled.
     *
     * @param verifyHost whether the hostname verification is enabled.
     * @return the current builder with the hostname verification set.
     */
    QuarkusRestClientBuilder verifyHost(boolean verifyHost);

    /**
     * Set the client-side trust store.
     *
     * @param trustStore key store
     * @return the current builder with the trust store set
     * @throws NullPointerException if the <code>trustStore</code> parameter is null.
     */
    QuarkusRestClientBuilder trustStore(KeyStore trustStore);

    /**
     * Set the client-side trust store.
     *
     * @param trustStore key store
     * @param trustStorePassword the password for the specified <code>trustStore</code>
     * @return the current builder with the trust store set
     * @throws NullPointerException if the <code>trustStore</code> parameter is null.
     */
    QuarkusRestClientBuilder trustStore(KeyStore trustStore, String trustStorePassword);

    /**
     * Set the client-side key store.
     *
     * @param keyStore key store
     * @param keystorePassword the password for the specified <code>keyStore</code>
     * @return the current builder with the key store set
     * @throws NullPointerException if the <code>keyStore</code> parameter is null.
     */
    QuarkusRestClientBuilder keyStore(KeyStore keyStore, String keystorePassword);

    /**
     * Set the hostname verifier to verify the endpoint's hostname
     *
     * @param hostnameVerifier the hostname verifier
     * @return the current builder with hostname verifier set
     * @throws NullPointerException if the <code>hostnameVerifier</code> parameter is null.
     */
    QuarkusRestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier);

    /**
     * Specifies whether client built by this builder should follow HTTP redirect responses (30x) or not.
     *
     * @param follow true if the client should follow HTTP redirects, false if not.
     * @return the current builder with the followRedirect property set.
     */
    QuarkusRestClientBuilder followRedirects(boolean follow);

    /**
     * Specifies the HTTP proxy hostname/IP address and port to use for requests from client instances.
     *
     * @param proxyHost hostname or IP address of proxy server - must be non-null
     * @param proxyPort port of proxy server
     * @throws IllegalArgumentException if the <code>proxyHost</code> is null or the <code>proxyPort</code> is invalid
     * @return the current builder with the proxy host set
     */
    QuarkusRestClientBuilder proxyAddress(String proxyHost, int proxyPort);

    /**
     * Specifies the proxy username.
     *
     * @param proxyUser the proxy username.
     * @return the current builder
     */
    QuarkusRestClientBuilder proxyUser(String proxyUser);

    /**
     * Specifies the proxy password.
     *
     * @param proxyPassword the proxy password.
     * @return the current builder
     */
    QuarkusRestClientBuilder proxyPassword(String proxyPassword);

    /**
     * Specifies the hosts to access without proxy.
     *
     * @param nonProxyHosts the hosts to access without proxy.
     * @return the current builder
     */
    QuarkusRestClientBuilder nonProxyHosts(String nonProxyHosts);

    /**
     * Specifies the URI formatting style to use when multiple query parameter values are passed to the client.
     *
     * @param style the URI formatting style to use for multiple query parameter values
     * @return the current builder with the style of query params set
     */
    QuarkusRestClientBuilder queryParamStyle(QueryParamStyle style);

    /**
     * Specifies the client headers factory to use.
     *
     * @param clientHeadersFactoryClass the client headers factory class to use.
     * @return the current builder
     */
    QuarkusRestClientBuilder clientHeadersFactory(Class<? extends ClientHeadersFactory> clientHeadersFactoryClass);

    /**
     * Specifies the client headers factory to use.
     *
     * @param clientHeadersFactory the client headers factory to use.
     * @return the current builder
     */
    QuarkusRestClientBuilder clientHeadersFactory(ClientHeadersFactory clientHeadersFactory);

    /**
     * Specifies the HTTP client options to use.
     *
     * @param httpClientOptionsClass the HTTP client options to use.
     * @return the current builder
     */
    QuarkusRestClientBuilder httpClientOptions(Class<? extends HttpClientOptions> httpClientOptionsClass);

    /**
     * Specifies the HTTP client options to use.
     *
     * @param httpClientOptions the HTTP client options to use.
     * @return the current builder
     */
    QuarkusRestClientBuilder httpClientOptions(HttpClientOptions httpClientOptions);

    /**
     * Specifies the client logger to use.
     *
     * @param clientLogger the client logger to use.
     * @return the current builder
     */
    QuarkusRestClientBuilder clientLogger(ClientLogger clientLogger);

    /**
     * Specifies the client logger to use.
     *
     * @param loggingScope to use
     * @return the current builder
     */
    QuarkusRestClientBuilder loggingScope(LoggingScope loggingScope);

    /**
     * How many characters of the body should be logged. Message body can be large and can easily pollute the logs.
     *
     */
    QuarkusRestClientBuilder loggingBodyLimit(Integer limit);

    /**
     * Enable trusting all certificates. Disable by default.
     */
    QuarkusRestClientBuilder trustAll(boolean trustAll);

    /**
     * Based on the configured QuarkusRestClientBuilder, creates a new instance of the given REST interface to invoke API calls
     * against.
     *
     * @param clazz the interface that defines REST API methods for use
     * @param <T> the type of the interface
     * @return a new instance of an implementation of this REST interface that
     * @throws IllegalStateException
     *         if not all pre-requisites are satisfied for the builder, this exception may get thrown. For instance,
     *         if the base URI/URL has not been set.
     * @throws RestClientDefinitionException if the passed-in interface class is invalid.
     */
    <T> T build(Class<T> clazz) throws IllegalStateException, RestClientDefinitionException;
}
