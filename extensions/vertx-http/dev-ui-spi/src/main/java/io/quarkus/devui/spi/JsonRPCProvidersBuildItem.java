package io.quarkus.devui.spi;

/**
 * This allows you to register a class that will provide data during runtime for JsonRPC Requests
 */
public final class JsonRPCProvidersBuildItem extends AbstractDevUIBuildItem {

    private final Class jsonRPCMethodProviderClass;

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass) {
        super();
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
    }

    public JsonRPCProvidersBuildItem(String customIdentifier, Class jsonRPCMethodProviderClass) {
        super(customIdentifier);
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
    }

    public Class getJsonRPCMethodProviderClass() {
        return jsonRPCMethodProviderClass;
    }
}
