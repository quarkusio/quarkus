package io.quarkus.vault.transit;

import java.time.OffsetDateTime;

public abstract class VaultTransitKeyVersion {

    private OffsetDateTime creationTime;

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public VaultTransitKeyVersion setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
        return this;
    }
}
