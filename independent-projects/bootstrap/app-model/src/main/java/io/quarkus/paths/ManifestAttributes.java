package io.quarkus.paths;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Manifests for some libraries can be quite large (e.g. for commons-codec, it is 21 KB).
 * <p>
 * Given we keep a lot of ArchivePathTree around, it seems like a good idea to only
 * keep around the Manifest entries that we actually use in Quarkus.
 * <p>
 * This can be extended further in the future if we need more attributes.
 */
public class ManifestAttributes {

    private final String specificationTitle;
    private final String specificationVersion;
    private final String specificationVendor;

    private final String implementationTitle;
    private final String implementationVersion;
    private final String implementationVendor;

    private final boolean multiRelease;

    private final String automaticModuleName;
    private final String mainClassName;
    private final Map<String, Set<String>> addExports;
    private final Map<String, Set<String>> addOpens;
    private final boolean enableNativeAccess;

    /**
     * {@return the manifest attributes for the given manifest, or {@code null} if the given value is {@code null}}
     *
     * @param manifest the manifest
     */
    public static ManifestAttributes of(Manifest manifest) {
        if (manifest == null) {
            return null;
        }

        return new ManifestAttributes(manifest);
    }

    private ManifestAttributes(Manifest manifest) {
        specificationTitle = manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE);
        specificationVersion = manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_VERSION);
        specificationVendor = manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_VENDOR);
        implementationTitle = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        implementationVersion = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        implementationVendor = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

        multiRelease = Boolean.parseBoolean(manifest.getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE));

        automaticModuleName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
        mainClassName = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

        addExports = parseAccess(manifest.getMainAttributes().getValue("Add-Exports"));
        addOpens = parseAccess(manifest.getMainAttributes().getValue("Add-Opens"));

        enableNativeAccess = "ALL-UNNAMED".equals(manifest.getMainAttributes().getValue("Enable-Native-Access"));
    }

    private static Map<String, Set<String>> parseAccess(final String value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (String item : value.split("\\s+")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }
            int idx = item.indexOf('/');
            if (idx == -1) {
                continue;
            }
            result.computeIfAbsent(item.substring(0, idx), ManifestAttributes::newHashSet).add(item.substring(idx + 1));
        }
        result.replaceAll((key, set) -> Set.copyOf(set));
        return Map.copyOf(result);
    }

    private static <E> HashSet<E> newHashSet(Object ignored) {
        return new HashSet<>();
    }

    /**
     * {@return the value of the {@code Specification-Title} attribute, or {@code null} if it is absent}
     */
    public String getSpecificationTitle() {
        return specificationTitle;
    }

    /**
     * {@return the value of the {@code Specification-Version} attribute, or {@code null} if it is absent}
     */
    public String getSpecificationVersion() {
        return specificationVersion;
    }

    /**
     * {@return the value of the {@code Specification-Vendor} attribute, or {@code null} if it is absent}
     */
    public String getSpecificationVendor() {
        return specificationVendor;
    }

    /**
     * {@return the value of the {@code Implementation-Title} attribute, or {@code null} if it is absent}
     */
    public String getImplementationTitle() {
        return implementationTitle;
    }

    /**
     * {@return the value of the {@code Implementation-Version} attribute, or {@code null} if it is absent}
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * {@return the value of the {@code Implementation-Vendor} attribute, or {@code null} if it is absent}
     */
    public String getImplementationVendor() {
        return implementationVendor;
    }

    /**
     * {@return the value of the {@code Multi-Release} attribute, or {@code false} if it is absent}
     */
    public boolean isMultiRelease() {
        return multiRelease;
    }

    /**
     * {@return the value of the {@code Automatic-Module-Name} attribute, or {@code null} if it is absent}
     */
    public String automaticModuleName() {
        return automaticModuleName;
    }

    /**
     * {@return the value of the {@code Main-Class} attribute, or {@code null} if it is absent}
     */
    public String mainClassName() {
        return mainClassName;
    }

    /**
     * {@return the parsed value of the {@code Add-Exports} attribute, or an empty map if it is absent}
     * The returned map keys represent the module to export from, and the values represent the corresponding
     * package sets to export from each module.
     * The returned map is immutable and contains no {@code null} keys or values.
     */
    public Map<String, Set<String>> addExports() {
        return addExports;
    }

    /**
     * {@return the parsed value of the {@code Add-Opens} attribute, or an empty map if it is absent}
     * The returned map keys represent the module to open from, and the values represent the corresponding
     * package sets to open from each module.
     * The returned map is immutable and contains no {@code null} keys or values.
     */
    public Map<String, Set<String>> addOpens() {
        return addOpens;
    }

    /**
     * {@return {@code true} if the value of the {@code Enable-Native-Access} attribute is present and equal to
     * {@code ALL-UNNAMED} or {@code false} otherwise}
     */
    public boolean enableNativeAccess() {
        return enableNativeAccess;
    }
}
