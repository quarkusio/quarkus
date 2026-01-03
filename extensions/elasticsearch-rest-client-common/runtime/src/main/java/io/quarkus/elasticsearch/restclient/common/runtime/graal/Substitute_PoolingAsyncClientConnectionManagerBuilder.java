package io.quarkus.elasticsearch.restclient.common.runtime.graal;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionOperator;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Original builder checks whether to use {@code ConscryptClientTlsStrategy} if it's on JDK <= 8 and has {@code org.conscrypt}
 * on a
 * classpath.
 * That condition will not be true here as we target JDK 17+, so we replace the impl and remove the check entirely.
 */
@TargetClass(className = "org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder")
final class Substitute_PoolingAsyncClientConnectionManagerBuilder {

    @Alias
    private TlsStrategy tlsStrategy;
    @Alias
    private SchemePortResolver schemePortResolver;
    @Alias
    private DnsResolver dnsResolver;
    @Alias
    private PoolConcurrencyPolicy poolConcurrencyPolicy;
    @Alias
    private PoolReusePolicy poolReusePolicy;

    @Alias
    private boolean systemProperties;

    @Alias
    private int maxConnTotal;
    @Alias
    private int maxConnPerRoute;

    @Alias
    private Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver;
    @Alias
    private Resolver<HttpHost, TlsConfig> tlsConfigResolver;
    @Alias
    private boolean messageMultiplexing;

    @Alias
    protected AsyncClientConnectionOperator createConnectionOperator(
            final TlsStrategy tlsStrategy,
            final SchemePortResolver schemePortResolver,
            final DnsResolver dnsResolver) {
        throw new UnsupportedOperationException();
    }

    @Substitute
    public PoolingAsyncClientConnectionManager build() {
        final TlsStrategy tlsStrategyCopy;
        if (tlsStrategy != null) {
            tlsStrategyCopy = tlsStrategy;
        } else {
            if (systemProperties) {
                tlsStrategyCopy = DefaultClientTlsStrategy.createSystemDefault();
            } else {
                tlsStrategyCopy = DefaultClientTlsStrategy.createDefault();
            }
        }
        final PoolingAsyncClientConnectionManager poolingmgr = new PoolingAsyncClientConnectionManager(
                createConnectionOperator(tlsStrategyCopy, schemePortResolver, dnsResolver),
                poolConcurrencyPolicy,
                poolReusePolicy,
                null,
                messageMultiplexing);
        poolingmgr.setConnectionConfigResolver(connectionConfigResolver);
        poolingmgr.setTlsConfigResolver(tlsConfigResolver);
        if (maxConnTotal > 0) {
            poolingmgr.setMaxTotal(maxConnTotal);
        }
        if (maxConnPerRoute > 0) {
            poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
        }
        return poolingmgr;
    }
}
