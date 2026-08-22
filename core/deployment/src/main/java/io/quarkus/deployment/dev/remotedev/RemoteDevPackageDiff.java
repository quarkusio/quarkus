package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;

/**
 * Immutable package changes to deliver in one remote-development operation.
 * <p>
 * Construction orders changed files deterministically, with the application-model file last because delivering that
 * file restarts the remote application. Deletion candidates are normalized to forward slashes, filtered through
 * {@link RemoteDevPackageDeletePolicy}, and sorted. Candidates rejected by that policy are omitted rather than
 * reported as deletions.
 * <p>
 * <strong>API note:</strong>
 * This value is an internal Quarkus build-tool integration contract. It is not a general filesystem difference
 * abstraction.
 *
 * @param changed non-{@code null} changed files; copied into deterministic delivery order
 * @param deleted non-{@code null} package-relative deletion candidates; normalized, conservatively filtered, sorted,
 *        and copied
 */
public record RemoteDevPackageDiff(
        List<RemoteDevPackageChange> changed,
        List<String> deleted) {

    static final String APPMODEL = "lib/deployment/appmodel.dat";

    /**
     * Normalizes and creates immutable copies of both lists.
     *
     * @throws NullPointerException if either list, a changed entry, or a deletion path is {@code null}
     */
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

    /**
     * Returns whether there is no accepted changed file or deletion to send.
     *
     * @return {@code true} when both normalized lists are empty
     */
    public boolean isEmpty() {
        return changed.isEmpty() && deleted.isEmpty();
    }

    private static Comparator<RemoteDevPackageChange> changeOrder() {
        return Comparator
                .comparing((RemoteDevPackageChange change) -> APPMODEL.equals(change.relativePath()))
                .thenComparing(RemoteDevPackageChange::relativePath);
    }
}
