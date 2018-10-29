package org.jboss.shamrock.deployment.cdi;

import org.jboss.builder.item.MultiBuildItem;

public final class GeneratedBeanBuildItem extends MultiBuildItem {

    final String name;
    final byte[] data;

    public GeneratedBeanBuildItem(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
