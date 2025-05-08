package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a JAX-RS resource that is generated.
 * Meant to be used by extension that generate JAX-RS resources as part of their build time processing
 */
public final class GeneratedJaxRsResourceBuildItem extends MultiBuildItem {

    private final String binaryName;
    private final String internalName;
    private final byte[] data;

    public GeneratedJaxRsResourceBuildItem(String name, byte[] data) {
        this.internalName = name.replace('.', '/');
        this.binaryName = name.replace('/', '.');
        this.data = data;
    }

    /**
     * @return the internal name for this class
     *
     * @deprecated Use {@link #internalName()} instead.
     */
    @Deprecated(since = "3.23", forRemoval = true)
    public String getName() {
        return internalName;
    }

    /**
     * {@return the <em>binary name</em> of the class, which is delimited by <code>.</code> characters}
     */
    public String binaryName() {
        return binaryName;
    }

    /**
     * {@return the <em>internal name</em> of the class, which is delimited by <code>/</code> characters}
     */
    public String internalName() {
        return internalName;
    }

    public byte[] getData() {
        return data;
    }
}
