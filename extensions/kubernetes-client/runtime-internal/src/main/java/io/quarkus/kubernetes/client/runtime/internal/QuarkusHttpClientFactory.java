package io.quarkus.kubernetes.client.runtime.internal;

import java.io.Closeable;
import java.util.List;

import jakarta.enterprise.inject.spi.CDI;

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

    private record VertxContext(Vertx vertx, boolean owned) {
    }

    // used a threadLocal in case several threads are creating their own QuarkusHttpClientFactory
    // if it's impossible it would be cheaper to just use a static field
    private static final ThreadLocal<VertxContext> VERTX_CONTEXT = new ThreadLocal<>();

    private final Vertx ownedVertx;

    public QuarkusHttpClientFactory() {
        super(prepareVertx());
        VertxContext ctx = VERTX_CONTEXT.get();
        VERTX_CONTEXT.remove();
        this.ownedVertx = ctx.owned() ? ctx.vertx() : null;
    }

    private static Vertx prepareVertx() {
        // at runtime CDI is available
        // at builtime it is not
        try {
            Vertx v = CDI.current().select(Vertx.class).get();
            VERTX_CONTEXT.set(new VertxContext(v, false));
            return v;
        } catch (Exception e) {
            Vertx v = createVertxInstance();
            VERTX_CONTEXT.set(new VertxContext(v, true));
            return v;
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
        org.eclipse.microprofile.config.Config cfg = io.smallrye.config.Config.get();
        io.vertx.core.net.PqcEnforcementPolicy vertxPolicy = cfg
                .getOptionalValue("quarkus.kubernetes-client.pqc-enforcement-policy",
                        io.vertx.core.net.PqcEnforcementPolicy.class)
                .orElse(io.vertx.core.net.PqcEnforcementPolicy.RELAXED);
        List<String> groups = cfg
                .getOptionalValues("quarkus.kubernetes-client.key-exchange-groups", String.class)
                .orElse(null);

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
