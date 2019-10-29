package io.quarkus.deployment.builditem.substrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a {@link java.lang.reflect.Proxy} definition
 * that will be required on substrate. This definition takes the form of an ordered
 * list of interfaces that this proxy will implement.
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem
 *             NativeImageProxyDefinitionBuildItem} instead.
 */
@Deprecated
public final class SubstrateProxyDefinitionBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public SubstrateProxyDefinitionBuildItem(String... classes) {
        this.classes = Arrays.asList(classes);
    }

    public SubstrateProxyDefinitionBuildItem(List<String> classes) {
        this.classes = new ArrayList<>(classes);
    }

    public List<String> getClasses() {
        return classes;
    }

}
