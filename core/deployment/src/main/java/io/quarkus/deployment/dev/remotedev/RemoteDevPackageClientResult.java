package io.quarkus.deployment.dev.remotedev;

import java.util.Set;

public record RemoteDevPackageClientResult(
        String outcome,
        int requested,
        int changed,
        int deleted,
        Set<String> requestedPaths) {

    public RemoteDevPackageClientResult {
        requestedPaths = Set.copyOf(requestedPaths);
    }

    public static RemoteDevPackageClientResult connected(Set<String> requestedPaths) {
        return new RemoteDevPackageClientResult("CONNECTED", requestedPaths.size(), 0, 0, requestedPaths);
    }

    public static RemoteDevPackageClientResult sent(int changed, int deleted) {
        return new RemoteDevPackageClientResult("SENT", 0, changed, deleted, Set.of());
    }
}
