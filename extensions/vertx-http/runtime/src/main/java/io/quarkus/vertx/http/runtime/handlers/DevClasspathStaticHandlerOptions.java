package io.quarkus.vertx.http.runtime.handlers;

import java.nio.charset.Charset;

import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;

public record DevClasspathStaticHandlerOptions(HttpBuildTimeConfig httpBuildTimeConfig, String indexPage,
        Charset defaultEncoding) {

    public static class Builder {
        private HttpBuildTimeConfig httpBuildTimeConfig;
        private String indexPage;
        private Charset contentEncoding;

        public Builder indexPage(String indexPage) {
            this.indexPage = indexPage;
            return this;
        }

        public Builder defaultEncoding(Charset contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        public Builder httpBuildTimeConfig(HttpBuildTimeConfig httpBuildTimeConfig) {
            this.httpBuildTimeConfig = httpBuildTimeConfig;
            return this;
        }

        public DevClasspathStaticHandlerOptions build() {
            return new DevClasspathStaticHandlerOptions(this.httpBuildTimeConfig, this.indexPage,
                    this.contentEncoding);
        }

    }
}
