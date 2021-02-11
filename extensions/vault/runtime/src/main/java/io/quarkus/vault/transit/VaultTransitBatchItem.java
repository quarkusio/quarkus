package io.quarkus.vault.transit;

/**
 * Base class for batch request items.
 */
public abstract class VaultTransitBatchItem {

    private TransitContext transitContext;

    public VaultTransitBatchItem(TransitContext transitContext) {
        this.transitContext = transitContext;
    }

    public byte[] getContext() {
        return transitContext == null ? null : transitContext.getContext();
    }

}
