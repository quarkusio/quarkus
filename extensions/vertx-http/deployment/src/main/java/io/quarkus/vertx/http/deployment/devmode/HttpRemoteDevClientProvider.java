package io.quarkus.vertx.http.deployment.devmode;

import java.util.Optional;

import io.quarkus.deployment.dev.remote.RemoteDevClient;
import io.quarkus.deployment.dev.remote.RemoteDevClientProvider;
import io.quarkus.runtime.LiveReloadConfig;
import io.smallrye.config.Config;

public class HttpRemoteDevClientProvider implements RemoteDevClientProvider {
    @Override
    public Optional<RemoteDevClient> getClient() {
        Config config = Config.get();
        LiveReloadConfig liveReloadConfig = config.getConfigMapping(LiveReloadConfig.class);
        if (!liveReloadConfig.url().isPresent()) {
            return Optional.empty();
        }
        if (!liveReloadConfig.password().isPresent()) {
            throw new RuntimeException(
                    "Live reload URL set but no password, remote dev requires a password, set quarkus.live-reload.password on both server and client");
        }
        return Optional.of(new HttpRemoteDevClient(liveReloadConfig.url().get(), liveReloadConfig.password().get(),
                liveReloadConfig.connectTimeout(), liveReloadConfig.retryInterval(), liveReloadConfig.retryMaxAttempts()));
    }
}
