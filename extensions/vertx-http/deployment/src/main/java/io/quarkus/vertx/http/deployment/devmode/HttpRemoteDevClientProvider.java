package io.quarkus.vertx.http.deployment.devmode;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.dev.remote.RemoteDevClient;
import io.quarkus.deployment.dev.remote.RemoteDevClientProvider;
import io.quarkus.runtime.LiveReloadConfig;
import io.quarkus.runtime.configuration.ConfigInstantiator;

public class HttpRemoteDevClientProvider implements RemoteDevClientProvider {

    private static final Logger log = Logger.getLogger(HttpRemoteDevClientProvider.class);

    /**
     * Used for remote dev mode, a bit of a hack to expose the config to the client
     */
    public static volatile LiveReloadConfig liveReloadConfig;

    @Override
    public Optional<RemoteDevClient> getClient() {
        if (liveReloadConfig == null) {
            liveReloadConfig = new LiveReloadConfig();
            ConfigInstantiator.handleObject(liveReloadConfig);
        }
        if (!liveReloadConfig.url.isPresent()) {
            return Optional.empty();
        }
        if (!liveReloadConfig.password.isPresent()) {
            throw new RuntimeException(
                    "Live reload URL set but no password, remote dev requires a password, set quarkus.live-reload.password on both server and client");
        }
        return Optional.of(new HttpRemoteDevClient(liveReloadConfig.url.get(), liveReloadConfig.password.get(),
                liveReloadConfig.connectTimeout, liveReloadConfig.retryInterval, liveReloadConfig.retryMaxAttempts));
    }
}
