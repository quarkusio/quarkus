package io.quarkus.kubernetes.client.runtime.internal;

import java.io.Closeable;
import java.util.List;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.vertx5.Vertx5HttpClientFactory;
import io.quarkus.runtime.ResettableSystemProperties;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.impl.SysProps;
import io.vertx.core.net.SSLOptions;
import io.vertx.ext.web.client.WebClientOptions;

public class QuarkusHttpClientFactory extends Vertx5HttpClientFactory implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(QuarkusHttpClientFactory.class);

    private record VertxContext(Vertx vertx, boolean owned) {
    }

    private final Vertx ownedVertx;

    public QuarkusHttpClientFactory() {
        this(resolveVertx());
    }

    private QuarkusHttpClientFactory(VertxContext ctx) {
        super(ctx.vertx());
        this.ownedVertx = ctx.owned() ? ctx.vertx() : null;
    }

    private static VertxContext resolveVertx() {
        // At runtime, reuse the Quarkus-managed Vert.x so all I/O shares the same event loop.
        // At build time (KubernetesClientBuildStep) and in mock-server test contexts, CDI is
        // absent or the Vert.x synthetic bean is not yet initialized, so we fall back to a
        // standalone instance that we own and must close ourselves.
        try {
            return new VertxContext(CDI.current().select(Vertx.class).get(), false);
        } catch (Exception e) {
            return new VertxContext(createVertxInstance(), true);
        }
    }

    private static Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        try (var resettableSystemProperties = ResettableSystemProperties.of(
                SysProps.DISABLE_DNS_RESOLVER.name, "true")) {
            return Vertx.vertx(new VertxOptions().setFileSystemOptions(
                    new FileSystemOptions().setFileCachingEnabled(false).setClassPathResolvingEnabled(false)));

        }
    }

    @Override
    protected void additionalConfig(WebClientOptions httpOptions, WebSocketClientOptions wsOptions,
            PoolOptions poolOptions) {
        org.eclipse.microprofile.config.Config cfg;
        try {
            cfg = io.smallrye.config.Config.get();
        } catch (IllegalArgumentException e) {
            // No configuration available for this classloader (e.g. mock server test context)
            return;
        }
        io.vertx.core.net.PqcEnforcementPolicy vertxPolicy = cfg
                .getOptionalValue("quarkus.kubernetes-client.pqc-enforcement-policy",
                        io.vertx.core.net.PqcEnforcementPolicy.class)
                .orElse(io.vertx.core.net.PqcEnforcementPolicy.RELAXED);
        List<String> groups = cfg
                .getOptionalValues("quarkus.kubernetes-client.key-exchange-groups", String.class)
                .orElse(null);

        if (groups != null && vertxPolicy == io.vertx.core.net.PqcEnforcementPolicy.RELAXED) {
            LOGGER.warn("quarkus.kubernetes-client.key-exchange-groups is set but pqc-enforcement-policy is 'relaxed'."
                    + " Use 'strict' or 'client-negotiated' to enforce post-quantum key exchange.");
        }

        applyPqcSettings(httpOptions.getSslOptions(), vertxPolicy, groups);
        applyPqcSettings(wsOptions.getSslOptions(), vertxPolicy, groups);
    }

    private static void applyPqcSettings(SSLOptions opts, io.vertx.core.net.PqcEnforcementPolicy policy,
            List<String> groups) {
        if (opts == null) {
            return;
        }
        opts.setPqcEnforcementPolicy(policy);
        if (groups != null) {
            opts.setKeyExchangeGroups(groups);
        }
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public void close() {
        if (ownedVertx != null) {
            ownedVertx.close();
        }
    }
}
