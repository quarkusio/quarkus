package io.quarkus.devui.spi;

import org.jboss.jandex.DotName;

/**
 * This allows you to register a class that will provide data during runtime for JsonRPC Requests.
 * <p>
 * Note that this build item should <em>not</em> be produced only in dev mode (e.g. using
 * {@code @BuildStep(onlyIf = IsDevelopment.class)}), because it is also used to discover
 * valid usages of execution model affecting annotations (see
 * {@link io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem}).
 */
public final class JsonRPCProvidersBuildItem extends AbstractDevUIBuildItem {

    private final Class jsonRPCMethodProviderClass;
    private final DotName defaultBeanScope;

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass) {
        super();
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
    }

    public JsonRPCProvidersBuildItem(Class jsonRPCMethodProviderClass, DotName defaultBeanScope) {
        super();
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = defaultBeanScope;
    }

    public JsonRPCProvidersBuildItem(String customIdentifier, Class jsonRPCMethodProviderClass) {
        super(customIdentifier);
        this.jsonRPCMethodProviderClass = jsonRPCMethodProviderClass;
        this.defaultBeanScope = null;
    }

    public Class getJsonRPCMethodProviderClass() {
        return jsonRPCMethodProviderClass;
    }

    public DotName getDefaultBeanScope() {
        return defaultBeanScope;
    }
}
