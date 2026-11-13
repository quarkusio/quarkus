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
    // created here rather than in the conversion build step so that the extension creating this build item is still
    // on the stack when the new build item detects the extension it belongs to
    private final io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem convertedItem;

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass) {
        this.customIdentifier = null;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
        this.convertedItem = new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(jsonRPCMethodProviderClass);
    }

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass, DotName defaultBeanScope) {
        this.customIdentifier = null;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = defaultBeanScope;
        this.convertedItem = new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(jsonRPCMethodProviderClass,
                defaultBeanScope);
    }

    public JsonRPCProvidersBuildItem(String customIdentifier, Class jsonRPCMethodProviderClass) {
        this.customIdentifier = customIdentifier;
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
        this.convertedItem = new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(customIdentifier,
                jsonRPCMethodProviderClass);
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

    /**
     * @return the equivalent {@link io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem}, attributed to the extension
     *         that created this build item
     */
    public io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem getConvertedItem() {
        return convertedItem;
    }
}
