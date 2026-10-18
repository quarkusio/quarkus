package io.quarkus.sbom;

import java.util.Objects;

/**
 * License information for a software component.
 * <p>
 * Holds a license name (e.g., an SPDX expression such as {@code "Apache-2.0"}) and an
 * optional URL pointing to the license text.
 *
 * @param name the license name (e.g., "Apache-2.0", "MIT"), never null
 * @param url URL pointing to the license text, or null
 */
public record LicenseInfo(String name, String url) {

    public static LicenseInfo forName(String name) {
        return new LicenseInfo(name, null);
    }

    /**
     * Creates a LicenseInfo with the given name and URL.
     *
     * @param name the license name
     * @param url the license URL, or null
     */
    public LicenseInfo {
        Objects.requireNonNull(name, "license name is required");
    }

    /**
     * Creates a LicenseInfo with the given name and no URL.
     *
     * @param name the license name
     */
    public LicenseInfo(String name) {
        this(name, null);
    }

    @Override
    public String toString() {
        return name;
    }
}
