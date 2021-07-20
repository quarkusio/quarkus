package io.quarkus.registry.catalog.selection;

import java.util.Objects;

/**
 * Extension origin preference
 */
public class OriginPreference implements Comparable<OriginPreference> {

    public OriginPreference(int registryPreference, int platformPreference, int releasePreference, int catalogPreference,
            int compatiblityCode) {
        this.registryPreference = registryPreference;
        this.platformPreference = platformPreference;
        this.releasePreference = releasePreference;
        this.catalogPreference = catalogPreference;
        this.compatibilityCode = compatiblityCode;
    }

    public final int registryPreference;
    public final int platformPreference;
    public final int releasePreference;
    public final int catalogPreference;
    public final int compatibilityCode;

    @Override
    public int compareTo(OriginPreference o) {
        int i = registryPreference - o.registryPreference;
        if (i != 0) {
            return i;
        }
        i = platformPreference - o.platformPreference;
        if (i != 0) {
            return i;
        }
        i = releasePreference - o.releasePreference;
        if (i != 0) {
            return i;
        }
        i = catalogPreference - o.catalogPreference;
        if (i != 0) {
            return i;
        }
        i = compatibilityCode - o.compatibilityCode;
        if (i != 0) {
            return i;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(releasePreference, platformPreference, compatibilityCode, registryPreference, catalogPreference);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OriginPreference other = (OriginPreference) obj;
        return releasePreference == other.releasePreference && compatibilityCode == other.compatibilityCode
                && platformPreference == other.platformPreference && registryPreference == other.registryPreference
                && catalogPreference == other.catalogPreference;
    }

    @Override
    public String toString() {
        return "[" + registryPreference + "." + platformPreference + "." + releasePreference + "." + catalogPreference + " "
                + compatibilityCode + "]";
    }
}
