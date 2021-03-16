package io.quarkus.bootstrap.runner;

public class ManifestInfo {

    final String specTitle;
    final String specVersion;
    final String specVendor;
    final String implTitle;
    final String implVersion;
    final String implVendor;

    public ManifestInfo(String specTitle, String specVersion, String specVendor, String implTitle, String implVersion,
            String implVendor) {
        this.specTitle = specTitle;
        this.specVersion = specVersion;
        this.specVendor = specVendor;
        this.implTitle = implTitle;
        this.implVersion = implVersion;
        this.implVendor = implVendor;
    }

    public String getSpecTitle() {
        return specTitle;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getSpecVendor() {
        return specVendor;
    }

    public String getImplTitle() {
        return implTitle;
    }

    public String getImplVersion() {
        return implVersion;
    }

    public String getImplVendor() {
        return implVendor;
    }
}
