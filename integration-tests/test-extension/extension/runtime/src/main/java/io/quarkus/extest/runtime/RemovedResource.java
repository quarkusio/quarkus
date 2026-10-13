package io.quarkus.extest.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import io.smallrye.common.net.URIs;

public enum RemovedResource {

    COMMON_NET_MESSAGES("io/smallrye/common/net/Messages.i18n.properties", URIs.class);

    private final String resourceName;
    private final Class<?> loadingClass;

    RemovedResource(String resourceName, Class<?> loadingClass) {
        this.resourceName = resourceName;
        this.loadingClass = loadingClass;
    }

    public String resourceName() {
        return resourceName;
    }

    public Class<?> loadingClass() {
        return loadingClass;
    }

    public enum ClassLoaderKind {
        OWN_CLASS_LOADER,
        CONTEXT_CLASS_LOADER;
    }

    public String load(ClassLoaderKind classLoaderKind) throws IOException {
        ClassLoader cl = classLoaderKind == ClassLoaderKind.OWN_CLASS_LOADER
                ? loadingClass.getClassLoader()
                : Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = cl.getResources(resourceName);
        StringBuilder sb = new StringBuilder();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream is = url.openStream()) {
                sb.append(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
