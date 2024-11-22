package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedClassBuildItem extends MultiBuildItem {

    final boolean applicationClass;
    final String name;
    String binaryName;
    String internalName;
    final byte[] classData;
    final String source;

    public GeneratedClassBuildItem(boolean applicationClass, String name, byte[] classData) {
        this(applicationClass, name, classData, null);
    }

    public GeneratedClassBuildItem(boolean applicationClass, String name, byte[] classData, String source) {
        if (name.startsWith("/")) {
            throw new IllegalArgumentException("Name cannot start with '/':" + name);
        }
        this.applicationClass = applicationClass;
        this.name = name;
        this.classData = classData;
        this.source = source;
    }

    public boolean isApplicationClass() {
        return applicationClass;
    }

    /**
     * {@return a name for this class}
     *
     * @deprecated This method may return the binary name, the internal name, or a hybrid thereof and should not be
     *             used. Use {@link #binaryName()} or {@link #internalName()} instead.
     */
    @Deprecated(forRemoval = true)
    public String getName() {
        return name;
    }

    /**
     * {@return the <em>binary name</em> of the class, which is delimited by <code>.</code> characters}
     */
    public String binaryName() {
        String binaryName = this.binaryName;
        if (binaryName == null) {
            binaryName = this.binaryName = name.replace('/', '.');
        }
        return binaryName;
    }

    /**
     * {@return the <em>internal name</em> of the class, which is delimited by <code>/</code> characters}
     */
    public String internalName() {
        String internalName = this.internalName;
        if (internalName == null) {
            internalName = this.internalName = name.replace('.', '/');
        }
        return internalName;
    }

    public byte[] getClassData() {
        return classData;
    }

    public String getSource() {
        return source;
    }

    public String toString() {
        return "GeneratedClassBuildItem[" + binaryName() + "]";
    }
}
