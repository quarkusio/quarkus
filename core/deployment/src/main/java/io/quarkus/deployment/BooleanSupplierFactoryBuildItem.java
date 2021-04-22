package io.quarkus.deployment;

import static io.quarkus.deployment.ExtensionLoader.reportError;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public final class BooleanSupplierFactoryBuildItem extends SimpleBuildItem {

    private final BuildTimeConfigurationReader.ReadResult readResult;
    private final LaunchMode launchMode;
    private final DevModeType devModeType;
    private final ClassValue<BooleanSupplier> suppliers = new ClassValue<BooleanSupplier>() {
        @Override
        protected BooleanSupplier computeValue(Class<?> type) {
            // construct a new supplier instance
            Consumer<BooleanSupplier> setup = o -> {
            };
            final Constructor<?>[] ctors = type.getDeclaredConstructors();
            if (ctors.length != 1) {
                throw reportError(type, "Conditional class must declare exactly one constructor");
            }
            final Constructor<?> ctor = ctors[0];
            ctor.setAccessible(true);
            List<Supplier<?>> paramSuppList = new ArrayList<>();
            for (Parameter parameter : ctor.getParameters()) {
                final Class<?> parameterClass = parameter.getType();
                if (parameterClass == LaunchMode.class) {
                    paramSuppList.add(() -> launchMode);
                } else if (parameterClass == DevModeType.class) {
                    paramSuppList.add(() -> devModeType);
                } else if (parameterClass.isAnnotationPresent(ConfigRoot.class)) {
                    final ConfigRoot annotation = parameterClass.getAnnotation(ConfigRoot.class);
                    final ConfigPhase phase = annotation.phase();
                    if (phase.isAvailableAtBuild()) {
                        paramSuppList.add(() -> readResult.requireRootObjectForClass(parameterClass));
                    } else if (phase.isReadAtMain()) {
                        throw reportError(parameter, phase + " configuration cannot be consumed here");
                    } else {
                        throw reportError(parameter,
                                "Unsupported conditional class configuration build phase " + phase);
                    }
                } else {
                    throw reportError(parameter,
                            "Unsupported conditional class constructor parameter type " + parameterClass);
                }
            }
            for (Field field : type.getDeclaredFields()) {
                final int fieldMods = field.getModifiers();
                if (Modifier.isStatic(fieldMods)) {
                    // ignore static fields
                    continue;
                }
                if (Modifier.isFinal(fieldMods)) {
                    // ignore final fields
                    continue;
                }
                if (!Modifier.isPublic(fieldMods) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                    field.setAccessible(true);
                }
                final Class<?> fieldClass = field.getType();
                if (fieldClass == LaunchMode.class) {
                    setup = setup.andThen(o -> ReflectUtil.setFieldVal(field, o, launchMode));
                } else if (fieldClass.isAnnotationPresent(ConfigRoot.class)) {
                    final ConfigRoot annotation = fieldClass.getAnnotation(ConfigRoot.class);
                    final ConfigPhase phase = annotation.phase();
                    if (phase.isAvailableAtBuild()) {
                        setup = setup.andThen(o -> ReflectUtil.setFieldVal(field, o,
                                readResult.requireRootObjectForClass(fieldClass)));
                    } else if (phase.isReadAtMain()) {
                        throw reportError(field, phase + " configuration cannot be consumed here");
                    } else {
                        throw reportError(field,
                                "Unsupported conditional class configuration build phase " + phase);
                    }
                } else {
                    throw reportError(field, "Unsupported conditional class field type " + fieldClass);
                }
            }
            // make it
            Object[] args = new Object[paramSuppList.size()];
            int idx = 0;
            for (Supplier<?> supplier : paramSuppList) {
                args[idx++] = supplier.get();
            }
            final BooleanSupplier bs;
            try {
                bs = (BooleanSupplier) ctor.newInstance(args);
            } catch (InstantiationException e) {
                throw ReflectUtil.toError(e);
            } catch (IllegalAccessException e) {
                throw ReflectUtil.toError(e);
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException | Error e2) {
                    throw e2;
                } catch (Throwable throwable) {
                    throw new IllegalStateException(throwable);
                }
            }
            setup.accept(bs);
            return bs;
        }
    };

    BooleanSupplierFactoryBuildItem(BuildTimeConfigurationReader.ReadResult readResult, LaunchMode launchMode,
            DevModeType devModeType) {
        this.readResult = readResult;
        this.launchMode = launchMode;
        this.devModeType = devModeType;
    }

    @SuppressWarnings("unchecked")
    public <T extends BooleanSupplier> T get(Class<T> type) {
        return (T) suppliers.get(type);
    }
}
