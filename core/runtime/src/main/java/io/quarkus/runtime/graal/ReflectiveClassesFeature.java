package io.quarkus.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import io.quarkus.runtime.NativeImageFeatureUtils;

public class ReflectiveClassesFeature implements Feature {

    public static final String META_INF_QUARKUS_NATIVE_REFLECT_DAT = "META-INF/quarkus-native-reflect.dat";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(META_INF_QUARKUS_NATIVE_REFLECT_DAT);
        if (resourceAsStream != null) {
            try (ObjectInputStream ois = new ObjectInputStream(resourceAsStream)) {
                for (int i = 0, length = ois.readInt(); i < length; i++) {
                    registerClass(
                            ois.readUTF(), ois.readBoolean(), ois.readBoolean(), ois.readBoolean(), ois.readBoolean(),
                            ois.readBoolean(), ois.readBoolean(), readStringCollection(ois, HashSet::new),
                            readReflectiveMethodInfoSet(ois),
                            readReflectiveMethodInfoSet(ois), access);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Register each class in " + META_INF_QUARKUS_NATIVE_REFLECT_DAT + " for reflection on Substrate VM";
    }

    private static <T extends Collection<String>> T readStringCollection(ObjectInputStream ois, Supplier<T> factory)
            throws IOException {
        T result = factory.get();
        for (int i = 0, length = ois.readInt(); i < length; i++) {
            result.add(ois.readUTF());
        }
        return result;
    }

    private static Set<ReflectiveMethodInfo> readReflectiveMethodInfoSet(ObjectInputStream ois) throws IOException {
        Set<ReflectiveMethodInfo> result = new HashSet<>();
        for (int i = 0, length = ois.readInt(); i < length; i++) {
            result.add(new ReflectiveMethodInfo(ois.readUTF(), ois.readUTF(), readStringCollection(ois, ArrayList::new)));
        }
        return result;
    }

    private static void registerClass(String className, boolean constructors, boolean methods,
            boolean fields, boolean finalFieldsWritable, boolean weak, boolean serialization,
            Set<String> fieldSet, Set<ReflectiveMethodInfo> methodSet, Set<ReflectiveMethodInfo> ctorSet,
            BeforeAnalysisAccess beforeAnalysisAccess) {
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Class<?> aClass = Class.forName(className, false, contextClassLoader);
            // we call these methods first, so if they are going to throw an exception it happens before anything has
            // been registered
            Constructor<?>[] declaredConstructors = aClass.getDeclaredConstructors();
            Method[] declaredMethods = aClass.getDeclaredMethods();
            Field[] declaredFields = aClass.getDeclaredFields();
            if (weak) {
                WeakReflection.register(beforeAnalysisAccess, aClass, constructors, methods, fields);
            } else {
                RuntimeReflection.register(aClass);
                if (constructors) {
                    RuntimeReflection.register(declaredConstructors);
                } else if (!ctorSet.isEmpty()) {
                    for (ReflectiveMethodInfo method : ctorSet) {
                        Class<?>[] paramArray = new Class<?>[method.params.size()];
                        for (int i = 0; i < paramArray.length; ++i) {
                            paramArray[i] = forName(method.params.get(i), contextClassLoader);
                        }
                        RuntimeReflection.register(aClass.getDeclaredConstructor(paramArray));
                    }
                }
                if (methods) {
                    RuntimeReflection.register(declaredMethods);
                } else if (!methodSet.isEmpty()) {
                    for (ReflectiveMethodInfo method : methodSet) {
                        Class<?>[] paramArray = new Class<?>[method.params.size()];
                        for (int i = 0; i < paramArray.length; ++i) {
                            paramArray[i] = forName(method.params.get(i), contextClassLoader);
                        }
                        RuntimeReflection.register(aClass.getDeclaredMethod(method.name, paramArray));
                    }
                }
                if (fields) {
                    RuntimeReflection.register(finalFieldsWritable, serialization, declaredFields);
                } else if (!fieldSet.isEmpty()) {
                    for (String field : fieldSet) {
                        RuntimeReflection.register(aClass.getDeclaredField(field));
                    }
                }
            }
            if (serialization) {
                Method method = NativeImageFeatureUtils.lookupMethod(RuntimeSerialization.class, "register", Class[].class);
                method.invoke(null, (Object) new Class[] { aClass });
            }
        } catch (Throwable e) {
            // e.printStackTrace();
        }
    }

    private static Class<?> forName(String className, ClassLoader loader) throws ClassNotFoundException {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                return Class.forName(className, false, loader);
        }
    }

    private static class ReflectiveMethodInfo {
        final String declaringClass;
        final String name;
        final List<String> params;

        ReflectiveMethodInfo(String declaringClass, String name, List<String> params) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.params = params;
        }
    }
}
