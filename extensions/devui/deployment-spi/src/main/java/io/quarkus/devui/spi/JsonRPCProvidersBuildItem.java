package io.quarkus.devui.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated Use {@link io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem} instead.
 */
@Deprecated(since = "4.0", forRemoval = true)
public final class JsonRPCProvidersBuildItem extends MultiBuildItem {

    private final String customIdentifier;
    private final Class jsonRPCMethodProviderClass;
    private final DotName defaultBeanScope;

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass) {
        this.customIdentifier = null;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
    }

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass, DotName defaultBeanScope) {
        this.customIdentifier = null;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = defaultBeanScope;
    }

    public JsonRPCProvidersBuildItem(String customIdentifier, Class jsonRPCMethodProviderClass) {
        this.customIdentifier = customIdentifier;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
    }

    public String getCustomIdentifier() {
        return customIdentifier;
    }

    public Class getJsonRPCMethodProviderClass() {
        return jsonRPCMethodProviderClass;
    }

    public DotName getDefaultBeanScope() {
        return defaultBeanScope;
    }
}
