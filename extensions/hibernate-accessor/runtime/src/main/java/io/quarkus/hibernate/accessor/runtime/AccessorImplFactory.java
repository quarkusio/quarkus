package io.quarkus.hibernate.accessor.runtime;

import java.lang.reflect.Constructor;

import org.hibernate.accessor.AccessorFactory;

/*
 * Connects transformed model classes to accessor implementations loaded through the application classloader.
 * The recorder initializes the factory and generated constructors before accessors are requested.
 *
 * There can be some entities (yes, Hibernate Search Outboxpolling Agent/Event ones come to mind ;))
 * that are contributed for accessor processing, but they would be living in the base classloader
 * not the application one. While the generated accessors... they all would be in the app classloader.
 * Hence, we cannot simply have them being used explicitly, and thus this indirection.
 */
public class AccessorImplFactory {

    private static volatile AccessorFactory factory;
    private static volatile Constructor<?> readerCtor;
    private static volatile Constructor<?> writerCtor;
    private static volatile Constructor<?> instantiatorCtor;

    public static void setFactory(AccessorFactory factory) {
        AccessorImplFactory.factory = factory;
    }

    public static AccessorFactory getFactory() {
        return factory;
    }

    public static void init(String readerClass, String writerClass, String instantiatorClass) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            readerCtor = cl.loadClass(readerClass).getDeclaredConstructor(int.class, int.class);
            writerCtor = cl.loadClass(writerClass).getDeclaredConstructor(int.class, int.class);
            instantiatorCtor = cl.loadClass(instantiatorClass).getDeclaredConstructor(int.class, int.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AccessorImplFactory", e);
        }
    }

    public static Object createReader(int classIndex, int memberIndex) {
        try {
            return readerCtor.newInstance(classIndex, memberIndex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create reader for classIndex=" + classIndex
                    + " memberIndex=" + memberIndex, e);
        }
    }

    public static Object createWriter(int classIndex, int memberIndex) {
        try {
            return writerCtor.newInstance(classIndex, memberIndex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create writer for classIndex=" + classIndex
                    + " memberIndex=" + memberIndex, e);
        }
    }

    public static Object createInstantiator(int classIndex, int memberIndex) {
        try {
            return instantiatorCtor.newInstance(classIndex, memberIndex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instantiator for classIndex=" + classIndex
                    + " memberIndex=" + memberIndex, e);
        }
    }
}
