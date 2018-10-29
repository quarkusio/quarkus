package org.jboss.shamrock.deployment.builditem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

public final class ProxyDefinitionBuildItem extends MultiBuildItem {

    private final List<String> classes;

    public ProxyDefinitionBuildItem(String... classes) {
        this.classes = Arrays.asList(classes);
    }
    public ProxyDefinitionBuildItem(List<String> classes) {
        this.classes = new ArrayList<>(classes);
    }


    public List<String> getClasses() {
        return classes;
    }

}
