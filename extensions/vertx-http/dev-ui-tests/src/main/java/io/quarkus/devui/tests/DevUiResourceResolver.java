package io.quarkus.devui.tests;

import java.net.URI;

import org.eclipse.microprofile.config.ConfigProvider;

public class DevUiResourceResolver {

    private final String host;
    private final String contextRoot;

    public DevUiResourceResolver() {
        this(
                ConfigProvider
                        .getConfig()
                        .getValue("test.url", String.class));
    }

    public DevUiResourceResolver(String host) {
        this.host = host;
        this.contextRoot = ConfigProvider
                .getConfig()
                .getOptionalValue("quarkus.http.non-application-root-path", String.class)
                .orElse("q");
    }

    private static String ensurePath(String path) {
        return (path.startsWith("/") ? "" : "/") + path;
    }

    public URI resolve(String path) {
        return URI.create(
                host
                        + ensurePath(contextRoot)
                        + ensurePath("dev-ui")
                        + ensurePath(path));
    }

}
