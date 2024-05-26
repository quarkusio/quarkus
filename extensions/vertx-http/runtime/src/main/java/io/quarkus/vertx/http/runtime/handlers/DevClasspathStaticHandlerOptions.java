package io.quarkus.vertx.http.runtime.handlers;

import java.nio.charset.Charset;
import java.util.Set;

public class DevClasspathStaticHandlerOptions {

    private final Set<String> compressMediaTypes;
    private final boolean enableCompression;
    private final String indexPage;
    private final Charset defaultEncoding;

    private DevClasspathStaticHandlerOptions(Set<String> compressMediaTypes, boolean enableCompression,
            String indexPage, Charset defaultEncoding) {
        this.compressMediaTypes = compressMediaTypes;
        this.enableCompression = enableCompression;
        this.indexPage = indexPage;
        this.defaultEncoding = defaultEncoding;
    }

    public Set<String> getCompressMediaTypes() {
        return compressMediaTypes;
    }

    public boolean isEnableCompression() {
        return enableCompression;
    }

    public String getIndexPage() {
        return indexPage;
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }

    public static class Builder {
        private Set<String> compressMediaTypes;
        private boolean enableCompression;
        private String indexPage;
        private Charset contentEncoding;

        public DevClasspathStaticHandlerOptions.Builder compressMediaTypes(Set<String> compressMediaTypes) {
            this.compressMediaTypes = compressMediaTypes;
            return this;
        }

        public DevClasspathStaticHandlerOptions.Builder enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public DevClasspathStaticHandlerOptions.Builder indexPage(String indexPage) {
            this.indexPage = indexPage;
            return this;
        }

        public DevClasspathStaticHandlerOptions.Builder defaultEncoding(Charset contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        public DevClasspathStaticHandlerOptions build() {
            return new DevClasspathStaticHandlerOptions(this.compressMediaTypes, this.enableCompression, this.indexPage,
                    this.contentEncoding);
        }

    }
}
