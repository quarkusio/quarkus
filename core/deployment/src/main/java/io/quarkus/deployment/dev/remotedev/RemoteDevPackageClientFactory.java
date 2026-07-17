package io.quarkus.deployment.dev.remotedev;

import java.io.IOException;

public interface RemoteDevPackageClientFactory {

    RemoteDevPackageClient create(RemoteDevPackageClientConfig config) throws IOException;
}
