package io.quarkus.bootstrap.resolver;

import java.io.Serializable;
import java.util.Objects;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;

/**
 * Serializable result that keeps an application model and its provider classification together for Quarkus-owned
 * Tooling API consumers.
 * <p>
 * A standalone result always carries a Gradle sidecar. An unmarked compatibility result never carries one; the
 * classification records only the absence of the standalone marker and does not identify a particular legacy provider
 * implementation. This value enforces that presence invariant, while {@link QuarkusToolingModelBuildAction}
 * correlation-validates a present sidecar before producing a standalone result.
 */
public final class QuarkusToolingModelResult implements Serializable {

    private static final long serialVersionUID = 7032723855628300508L;

    private final ApplicationModel applicationModel;
    private final ProviderKind providerKind;
    private final GradleApplicationModelSidecar sidecar;

    /**
     * Creates a paired Tooling API result.
     *
     * @param applicationModel fetched application model; must not be {@code null}
     * @param providerKind provider classification; must not be {@code null}
     * @param sidecar sidecar for {@link ProviderKind#STANDALONE_APPLICATION}, or {@code null} for
     *        {@link ProviderKind#UNMARKED_COMPATIBILITY}
     * @throws NullPointerException if {@code applicationModel} or {@code providerKind} is {@code null}
     * @throws IllegalArgumentException if the sidecar presence does not match {@code providerKind}
     */
    public QuarkusToolingModelResult(ApplicationModel applicationModel, ProviderKind providerKind,
            GradleApplicationModelSidecar sidecar) {
        this.applicationModel = Objects.requireNonNull(applicationModel, "applicationModel");
        this.providerKind = Objects.requireNonNull(providerKind, "providerKind");
        this.sidecar = sidecar;
        if (providerKind == ProviderKind.STANDALONE_APPLICATION && sidecar == null) {
            throw new IllegalArgumentException("The standalone application provider requires a Gradle sidecar");
        }
        if (providerKind == ProviderKind.UNMARKED_COMPATIBILITY && sidecar != null) {
            throw new IllegalArgumentException("An unmarked compatibility provider cannot carry a Gradle sidecar");
        }
    }

    /** @return fetched application model; never {@code null} */
    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    /** @return classification of the model provider; never {@code null} */
    public ProviderKind getProviderKind() {
        return providerKind;
    }

    /**
     * @return sidecar whose presence matches the provider classification, or {@code null} for an unmarked
     *         compatibility provider; this value enforces presence only, while
     *         {@link QuarkusToolingModelBuildAction} correlation-validates a returned sidecar before producing a
     *         standalone result
     */
    public GradleApplicationModelSidecar getSidecar() {
        return sidecar;
    }

    /** Provider classification used by Quarkus-owned Tooling API consumers. */
    public enum ProviderKind {
        /** The application model was available but no standalone-plugin sidecar marker was published. */
        UNMARKED_COMPATIBILITY,
        /** The standalone application plugin published its sidecar marker. */
        STANDALONE_APPLICATION
    }
}
