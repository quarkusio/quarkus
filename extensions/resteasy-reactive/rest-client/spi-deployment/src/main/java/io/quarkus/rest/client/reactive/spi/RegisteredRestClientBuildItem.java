package io.quarkus.rest.client.reactive.spi;

import static org.jboss.resteasy.reactive.common.processor.EndpointIndexer.CDI_WRAPPER_SUFFIX;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains information about the REST Clients that have been discovered via
 * {@code org.eclipse.microprofile.rest.client.inject.RegisterRestClient} or that have been given a CDI scope via
 * configuration.
 */
public final class RegisteredRestClientBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;
    private final Optional<String> configKey;
    private final Optional<String> defaultBaseUri;
    private final List<MethodInfo> cdiWrapperMethods;

    public RegisteredRestClientBuildItem(ClassInfo classInfo, Optional<String> configKey, Optional<String> defaultBaseUri,
            List<MethodInfo> cdiWrapperMethods) {
        this.classInfo = Objects.requireNonNull(classInfo);
        this.configKey = Objects.requireNonNull(configKey);
        this.defaultBaseUri = Objects.requireNonNull(defaultBaseUri);
        this.cdiWrapperMethods = List.copyOf(cdiWrapperMethods);
    }

    /**
     * @return the REST Client interface
     */
    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public Optional<String> getConfigKey() {
        return configKey;
    }

    public Optional<String> getDefaultBaseUri() {
        return defaultBaseUri;
    }

    /**
     * The interface methods implemented by the CDI bean generated for the REST Client interface, in the order
     * in which they are generated.
     * <p>
     * Each entry is the method as declared by the REST Client interface or by one of its super interfaces,
     * so {@link MethodInfo#declaringClass()} is the interface that actually declares the method.
     * The list is empty when no CDI bean is generated for the REST Client interface.
     *
     * @return an unmodifiable list of the methods implemented by the generated CDI bean
     */
    public List<MethodInfo> getCdiWrapperMethods() {
        return cdiWrapperMethods;
    }

    /**
     * @return the name of the CDI bean generated for the REST Client interface, see {@link #getCdiWrapperMethods()}
     */
    public DotName getCdiWrapperName() {
        return DotName.createSimple(classInfo.name().toString() + CDI_WRAPPER_SUFFIX);
    }
}
