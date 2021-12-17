package io.quarkus.registry.catalog.selection;

import io.quarkus.registry.catalog.ExtensionCatalog;

public class OriginWithPreference {

    private final ExtensionCatalog catalog;
    private final OriginPreference preference;

    public OriginWithPreference(ExtensionCatalog catalog, OriginPreference preference) {
        this.catalog = catalog;
        this.preference = preference;
    }

    public ExtensionCatalog getCatalog() {
        return catalog;
    }

    public OriginPreference getPreference() {
        return preference;
    }

    public boolean isSameAs(OriginWithPreference o) {
        return preference.equals(o.preference);
    }

    public boolean canBeCombinedWith(OriginWithPreference o) {
        return preference.canBeCombinedWith(o.getPreference());
    }
}
