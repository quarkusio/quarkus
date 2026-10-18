package io.quarkus.deployment.dev.remote;

import java.util.Optional;

public interface RemoteDevClientProvider {

    Optional<RemoteDevClient> getClient();
}
