package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents a {@link java.lang.reflect.Proxy} definition
 * that will be required in native mode. This definition takes the form of an ordered
 * list of interfaces that this proxy will implement.
 */
public final class NativeImageProxyDefinitionBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public NativeImageProxyDefinitionBuildItem(String... classes) {
        this.classes = Arrays.asList(classes);
    }

    public NativeImageProxyDefinitionBuildItem(List<String> classes) {
        this.classes = new ArrayList<>(classes);
    }

    public List<String> getClasses() {
        return classes;
    }

}
