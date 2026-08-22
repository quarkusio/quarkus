package io.quarkus.test.junit.common;

/**
 * A classloader that adds synthetic void stub methods for {@code @Test @TestTransaction}
 * methods returning {@code Uni}, so JUnit 5 discovers the stubs. At execution time, the
 * framework strips the suffix and invokes the original Uni-returning method.
 */
final class ReactiveTestTransformingClassLoader extends ClassLoader {

    ReactiveTestTransformingClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }

            if (!shouldCheck(name)) {
                return super.loadClass(name, resolve);
            }

            byte[] classBytes = ReactiveTestMethodTransformer.readClassBytes(name, getParent());
            if (classBytes == null) {
                return super.loadClass(name, resolve);
            }

            byte[] transformed = ReactiveTestMethodTransformer.transformIfNeeded(classBytes, getParent());
            if (transformed == null) {
                return super.loadClass(name, resolve);
            }

            Class<?> defined = defineClass(name, transformed, 0, transformed.length);
            if (resolve) {
                resolveClass(defined);
            }
            return defined;
        }
    }

    private static boolean shouldCheck(String name) {
        return !name.startsWith("java.")
                && !name.startsWith("javax.")
                && !name.startsWith("jakarta.")
                && !name.startsWith("jdk.")
                && !name.startsWith("sun.")
                && !name.startsWith("com.sun.")
                && !name.startsWith("org.junit.")
                && !name.startsWith("org.objectweb.")
                && !name.startsWith("org.jboss.")
                && !name.startsWith("io.quarkus.test.junit.");
    }
}
