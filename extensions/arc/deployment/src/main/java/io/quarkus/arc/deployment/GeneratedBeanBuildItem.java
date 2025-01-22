package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated CDI bean. If this is produced then a {@link io.quarkus.deployment.builditem.GeneratedClassBuildItem}
 * should not be produced for the same class, as Arc will take care of this.
 */
public final class GeneratedBeanBuildItem extends MultiBuildItem {

    private final boolean applicationClass;
    private final String name;
    private final byte[] data;
    private final String source;

    public GeneratedBeanBuildItem(String name, byte[] data) {
        this(name, data, null);
    }

    public GeneratedBeanBuildItem(String name, byte[] data, String source) {
        this(name, data, source, true);
    }

    public GeneratedBeanBuildItem(String name, byte[] data, String source, boolean applicationClass) {
        this.name = name;
        this.data = data;
        this.source = source;
        this.applicationClass = applicationClass;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    /**
     *
     * @return the textual representation of generated code
     */
    public String getSource() {
        return source;
    }

    public boolean isApplicationClass() {
        return applicationClass;
    }

}
