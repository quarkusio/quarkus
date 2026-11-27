package io.quarkus.vertx.http.runtime.handlers;

import java.nio.charset.Charset;

import io.quarkus.vertx.http.runtime.StaticResourcesConfig;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;

public record DevClasspathStaticHandlerOptions(VertxHttpBuildTimeConfig httpBuildTimeConfig, String indexPage,
        Charset defaultEncoding, StaticResourcesConfig.IndexDirectories indexDirectories) {

    public static class Builder {
        private VertxHttpBuildTimeConfig httpBuildTimeConfig;
        private String indexPage;
        private Charset contentEncoding;
        private StaticResourcesConfig.IndexDirectories indexDirectories = StaticResourcesConfig.IndexDirectories.NONE;

        public Builder indexDirectories(StaticResourcesConfig.IndexDirectories indexDirectories) {
            this.indexDirectories = indexDirectories;
            return this;
        }

        public Builder indexPage(String indexPage) {
            this.indexPage = indexPage;
            return this;
        }

        public Builder defaultEncoding(Charset contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        public Builder httpBuildTimeConfig(VertxHttpBuildTimeConfig httpBuildTimeConfig) {
            this.httpBuildTimeConfig = httpBuildTimeConfig;
            return this;
        }

        public DevClasspathStaticHandlerOptions build() {
            return new DevClasspathStaticHandlerOptions(this.httpBuildTimeConfig, this.indexPage,
                    this.contentEncoding, this.indexDirectories);
        }

    }
}
