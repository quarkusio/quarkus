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
        final OriginPreference otherPref = o.getPreference();
        // if the quarkus versions are not compatible
        if (preference.compatibilityCode != otherPref.compatibilityCode
                // if it's different releases of the same platform
                || preference.registryPreference == otherPref.registryPreference
                        && preference.platformPreference == otherPref.platformPreference
                        && preference.releasePreference != otherPref.releasePreference) {
            return false;
        }
        return true;
    }
}
