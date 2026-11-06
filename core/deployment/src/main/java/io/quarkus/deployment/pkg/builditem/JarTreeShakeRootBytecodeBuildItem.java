package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A class that isn't on the build classpath but is generated as part of the build and will end up
 * on the runtime classpath. The jar tree shaker treats it as a root class and traces its references.
 */
public final class JarTreeShakeRootBytecodeBuildItem extends MultiBuildItem {

    private final String className;
    private final byte[] bytecode;

    /**
     * @param className dot-separated class name
     * @param bytecode class bytecode
     */
    public JarTreeShakeRootBytecodeBuildItem(String className, byte[] bytecode) {
        this.className = className;
        this.bytecode = bytecode;
    }

    public String getClassName() {
        return className;
    }

    public byte[] getBytecode() {
        return bytecode;
    }
}
