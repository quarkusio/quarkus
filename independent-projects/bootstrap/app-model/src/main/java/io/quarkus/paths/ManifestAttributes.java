package io.quarkus.paths;

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

    private String specificationTitle;
    private String specificationVersion;
    private String specificationVendor;

    private String implementationTitle;
    private String implementationVersion;
    private String implementationVendor;

    private boolean multiRelease;

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
    }

    public String getSpecificationTitle() {
        return specificationTitle;
    }

    public String getSpecificationVersion() {
        return specificationVersion;
    }

    public String getSpecificationVendor() {
        return specificationVendor;
    }

    public String getImplementationTitle() {
        return implementationTitle;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public String getImplementationVendor() {
        return implementationVendor;
    }

    public boolean isMultiRelease() {
        return multiRelease;
    }
}
