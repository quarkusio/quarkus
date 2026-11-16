package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;

public record RemoteDevPackageDiff(
        List<RemoteDevPackageChange> changed,
        List<String> deleted) {

    static final String APPMODEL = "lib/deployment/appmodel.dat";

    public RemoteDevPackageDiff {
        changed = requireNonNull(changed, "changed").stream()
                .sorted(changeOrder())
                .toList();
        deleted = requireNonNull(deleted, "deleted").stream()
                .map(RemoteDevPackageDeletePolicy::normalize)
                .filter(RemoteDevPackageDeletePolicy::canDelete)
                .sorted()
                .toList();
    }

    public boolean isEmpty() {
        return changed.isEmpty() && deleted.isEmpty();
    }

    private static Comparator<RemoteDevPackageChange> changeOrder() {
        return Comparator
                .comparing((RemoteDevPackageChange change) -> APPMODEL.equals(change.relativePath()))
                .thenComparing(RemoteDevPackageChange::relativePath);
    }
}
