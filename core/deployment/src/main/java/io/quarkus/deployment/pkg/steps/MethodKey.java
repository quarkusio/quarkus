package io.quarkus.deployment.pkg.steps;

import java.util.Set;

/**
 * Structured key for a JVM method, storing the three components provided by ASM's
 * {@link org.objectweb.asm.MethodVisitor#visitMethodInsn} and
 * {@link org.objectweb.asm.ClassVisitor#visitMethod} without string concatenation.
 *
 * <p>
 * The three strings (owner, name, descriptor) are interned by ASM's
 * {@link org.objectweb.asm.ClassReader} from the class file constant pool, so
 * storing them as references avoids all allocation. Hash code is lazily cached
 * and computed from the three pre-cached {@link String#hashCode()} values.
 */
final class MethodKey {

    /** JDK methods that load classes by name — the seeds for call chain propagation. */
    static final Set<MethodKey> SEED_METHOD_KEYS = Set.of(
            new MethodKey("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;"),
            new MethodKey("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;"),
            new MethodKey("java/lang/ClassLoader", "findClass", "(Ljava/lang/String;)Ljava/lang/Class;"),
            new MethodKey("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;"),
            new MethodKey("java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"),
            new MethodKey("java/lang/Class", "forName", "(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;"),
            new MethodKey("java/lang/invoke/MethodHandles$Lookup", "findClass",
                    "(Ljava/lang/String;)Ljava/lang/Class;"));

    /**
     * Owner classes of the seed methods. Used for fast pre-filtering during caller
     * index construction: a method call to a JDK/infra class only needs full
     * {@link MethodKey} construction and seed set lookup if the owner is one of these.
     */
    private static final Set<String> SEED_METHOD_OWNERS = Set.of(
            "java/lang/ClassLoader",
            "java/lang/Class",
            "java/lang/invoke/MethodHandles$Lookup");

    final String owner;
    final String name;
    final String descriptor;
    private int hash;

    MethodKey(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    String ownerAsDotName() {
        return owner.replace('/', '.');
    }

    /**
     * Returns {@code true} if this method is a constructor ({@code <init>}) or
     * static initializer ({@code <clinit>}).
     */
    boolean isInitOrClinit() {
        return name.charAt(0) == '<' && name.endsWith("init>");
    }

    /**
     * Returns {@code true} if the given method owner is one of the JDK classes
     * that contain seed methods ({@code ClassLoader}, {@code Class}, or
     * {@code MethodHandles.Lookup}). Used during caller index construction to
     * avoid creating {@link MethodKey} objects for JDK/infra calls that cannot
     * possibly be seeds.
     */
    static boolean isSeedMethodOwner(String owner) {
        return SEED_METHOD_OWNERS.contains(owner);
    }

    /**
     * Checks whether an internal class name (or method key starting with a class name)
     * belongs to a JDK or infrastructure package that should be excluded from
     * class-loading chain propagation. Works on both bare owner names
     * ({@code java/lang/Class}) and full method keys ({@code java/lang/Class.forName(...)})
     * because the check is prefix-based.
     */
    static boolean isJdkOrInfraClass(String nameOrKey) {
        return nameOrKey.startsWith("java/")
                || nameOrKey.startsWith("javax/")
                || nameOrKey.startsWith("jakarta/")
                || nameOrKey.startsWith("org/objectweb/")
                || nameOrKey.startsWith("sun/");
    }

    @Override
    public String toString() {
        return owner + "." + name + descriptor;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = owner.hashCode() * 31 * 31 + name.hashCode() * 31 + descriptor.hashCode();
            if (h == 0) {
                h = 1;
            }
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MethodKey other) {
            return owner.equals(other.owner)
                    && name.equals(other.name)
                    && descriptor.equals(other.descriptor);
        }
        return false;
    }
}
