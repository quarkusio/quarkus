package io.quarkus.jdbc.h2.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.function.BiConsumer;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * Custom GraalVM feature to automatically register DataType and StatefulDataType
 * implementors for reflective access.
 * These are identified using Jandex, looking both into the H2 core jar and in
 * user's indexed code.
 */
public final class H2Reflections implements Feature {

    public static final String REZ_NAME_DATA_TYPE_SINGLETONS = "h2BasicDataTypeSingletons.classlist";
    public static final String REZ_NAME_STATEFUL_DATATYPES = "h2StatefulDataType.classlist";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> metaType = access.findClassByName("org.h2.mvstore.type.MetaType");
        access.registerReachabilityHandler(this::metaTypeReachable, metaType);
    }

    private void metaTypeReachable(DuringAnalysisAccess access) {
        //Register some common metatypes - these are dynamically loaded depending on the data content.
        register(REZ_NAME_DATA_TYPE_SINGLETONS, this::registerSingletonAccess, access);
        //Now register implementors of org.h2.mvstore.type.StatefulDataType.Factory
        register(REZ_NAME_STATEFUL_DATATYPES, this::registerForReflection, access);
    }

    void register(final String resourceName, BiConsumer<String, DuringAnalysisAccess> action, DuringAnalysisAccess access) {
        try (InputStream resource = access.getApplicationClassLoader().getResourceAsStream(resourceName)) {
            Scanner s = new Scanner(resource);
            while (s.hasNext()) {
                final String className = s.next();
                action.accept(className, access);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void registerSingletonAccess(String className, DuringAnalysisAccess access) {
        try {
            final Field instance = access.findClassByName(className)
                    .getDeclaredField("INSTANCE");
            RuntimeReflection.register(instance);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    void registerForReflection(
            String className,
            DuringAnalysisAccess duringAnalysisAccess) {
        final Class<?> aClass = duringAnalysisAccess.findClassByName(className);
        final Constructor<?>[] z = aClass.getDeclaredConstructors();
        RuntimeReflection.register(aClass);
        RuntimeReflection.register(z);
    }

    @Override
    public String getDescription() {
        return "Support for H2 Database's extended data types";
    }

}
