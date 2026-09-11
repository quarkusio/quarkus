package io.quarkus.deployment.dev.remotedev;

import static java.util.Objects.requireNonNull;

import java.util.Set;

/**
 * Immutable result of a package-client operation.
 * <p>
 * Counts are meaningful only for the matching {@link #outcome()}: {@link RemoteDevPackageClientOutcome#CONNECTED}
 * carries {@link #requested()} and {@link #requestedPaths()}, {@link RemoteDevPackageClientOutcome#SENT} carries
 * {@link #changed()} and {@link #deleted()}, and {@link RemoteDevPackageClientOutcome#RECONNECT_REQUIRED} carries no
 * package counts or paths.
 * <p>
 * <strong>API note:</strong>
 * This value is an internal Quarkus build-tool integration contract. Its shape is not guaranteed across different
 * Quarkus versions.
 *
 * @param outcome non-{@code null} operation outcome
 * @param requested number of paths requested during connection; zero for other outcomes
 * @param changed number of changed files sent; zero for outcomes other than {@code SENT}
 * @param deleted number of deletion requests sent; zero for outcomes other than {@code SENT}
 * @param requestedPaths non-{@code null} package-relative paths requested during connection; defensively copied
 */
public record RemoteDevPackageClientResult(
        RemoteDevPackageClientOutcome outcome,
        int requested,
        int changed,
        int deleted,
        Set<String> requestedPaths) {

    /**
     * Validates outcome-specific counts and creates an immutable copy of {@code requestedPaths}.
     *
     * @throws NullPointerException if {@code outcome}, {@code requestedPaths}, or a requested path is {@code null}
     * @throws IllegalArgumentException if a count is negative or is inconsistent with {@code outcome}
     */
    public RemoteDevPackageClientResult {
        requireNonNull(outcome, "outcome");
        requestedPaths = Set.copyOf(requireNonNull(requestedPaths, "requestedPaths"));
        if (requested < 0 || changed < 0 || deleted < 0) {
            throw new IllegalArgumentException("Remote-dev package result counts must not be negative");
        }
        switch (outcome) {
            case CONNECTED -> {
                if (requested != requestedPaths.size() || changed != 0 || deleted != 0) {
                    throw new IllegalArgumentException("CONNECTED result has inconsistent counts");
                }
            }
            case SENT -> {
                if (requested != 0 || !requestedPaths.isEmpty()) {
                    throw new IllegalArgumentException("SENT result must not contain requested paths");
                }
            }
            case RECONNECT_REQUIRED -> {
                if (requested != 0 || changed != 0 || deleted != 0 || !requestedPaths.isEmpty()) {
                    throw new IllegalArgumentException("RECONNECT_REQUIRED result must not contain package changes");
                }
            }
        }
    }

    /**
     * Creates a successful connection result.
     *
     * @param requestedPaths non-{@code null} package-relative paths requested by the endpoint
     * @return a {@link RemoteDevPackageClientOutcome#CONNECTED} result
     * @throws NullPointerException if {@code requestedPaths} or a requested path is {@code null}
     */
    public static RemoteDevPackageClientResult connected(Set<String> requestedPaths) {
        return new RemoteDevPackageClientResult(RemoteDevPackageClientOutcome.CONNECTED, requestedPaths.size(), 0, 0,
                requestedPaths);
    }

    /**
     * Creates a successful difference-delivery result.
     *
     * @param changed number of changed files sent
     * @param deleted number of deletions sent
     * @return a {@link RemoteDevPackageClientOutcome#SENT} result
     * @throws IllegalArgumentException if either count is negative
     */
    public static RemoteDevPackageClientResult sent(int changed, int deleted) {
        return new RemoteDevPackageClientResult(RemoteDevPackageClientOutcome.SENT, 0, changed, deleted, Set.of());
    }

    /**
     * Creates a result instructing the session owner to reconnect.
     *
     * @return a {@link RemoteDevPackageClientOutcome#RECONNECT_REQUIRED} result
     */
    public static RemoteDevPackageClientResult reconnectRequired() {
        return new RemoteDevPackageClientResult(RemoteDevPackageClientOutcome.RECONNECT_REQUIRED, 0, 0, 0, Set.of());
    }
}
