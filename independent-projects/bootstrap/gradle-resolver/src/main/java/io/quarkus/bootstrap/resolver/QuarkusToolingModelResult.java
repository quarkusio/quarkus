package io.quarkus.bootstrap.resolver;

import java.io.Serializable;
import java.util.Objects;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;

/**
 * Internal result used to keep the application model and its provider
 * ownership together while routing Quarkus-owned consumers.
 */
public final class QuarkusToolingModelResult implements Serializable {

    private static final long serialVersionUID = 7032723855628300508L;

    private final ApplicationModel applicationModel;
    private final ProviderKind providerKind;
    private final GradleApplicationModelSidecar sidecar;

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

    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public ProviderKind getProviderKind() {
        return providerKind;
    }

    public GradleApplicationModelSidecar getSidecar() {
        return sidecar;
    }

    public enum ProviderKind {
        UNMARKED_COMPATIBILITY,
        STANDALONE_APPLICATION
    }
}
