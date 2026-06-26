package io.quarkus.spring.boot.properties.deployment;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.arc.deployment.ConfigBuildStep;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.configuration.ArrayListFactory;
import io.quarkus.runtime.configuration.HashSetFactory;
import io.smallrye.config.Config;

final class ConfigurationPropertiesUtil {

    static final String PACKAGE_TO_PLACE_GENERATED_CLASSES = "io.quarkus.spring.boot.properties.runtime.config";

    private static final MethodDesc CONFIG_GET_VALUES_MAP = MethodDesc.of(
            Config.class, "getValues", Map.class, String.class, Class.class, Class.class);
    private static final MethodDesc CONFIG_GET_OPTIONAL_VALUES_MAP = MethodDesc.of(
            Config.class, "getOptionalValues", Optional.class, String.class, Class.class, Class.class);
    private static final MethodDesc CONFIG_GET_VALUES_COLLECTION = MethodDesc.of(
            Config.class, "getValues", Collection.class, String.class, Class.class, IntFunction.class);
    private static final MethodDesc CONFIG_GET_OPTIONAL_VALUES_COLLECTION = MethodDesc.of(
            Config.class, "getOptionalValues", Optional.class, String.class, Class.class, IntFunction.class);
    private static final MethodDesc CONFIG_GET_VALUE = MethodDesc.of(
            Config.class, "getValue", Object.class, String.class, Class.class);
    private static final MethodDesc CONFIG_GET_OPTIONAL_VALUE = MethodDesc.of(
            Config.class, "getOptionalValue", Optional.class, String.class, Class.class);

    private ConfigurationPropertiesUtil() {
    }

    /**
     * Generates code that uses Config#getValue for simple objects, or SmallRyeConfig#getValues if it is a Collection
     * type.
     *
     * @param propertyName Property name that needs to be fetched
     * @param resultType Type to which the property value needs to be converted to
     * @param declaringClass Config class where the type was encountered
     * @param bc Where the bytecode will be generated
     * @param config Reference to the MP config object
     */
    static Expr createReadMandatoryValueAndConvertIfNeeded(
            String propertyName,
            Type resultType,
            DotName declaringClass,
            BlockCreator bc,
            Expr config) {

        if (isMap(resultType)) {
            if (resultType.kind() != Kind.PARAMETERIZED_TYPE) {
                throw new IllegalArgumentException("Unable to resolve Map parameter types for " + propertyName);
            }

            ParameterizedType parameterizedType = resultType.asParameterizedType();
            String keyType = parameterizedType.arguments().get(0).name().toString();
            String valueType = parameterizedType.arguments().get(1).name().toString();

            // Only support Map#value with Converter
            // We don't have a way to validate, so it will fail at runtime (with a proper error message from SR Config)
            return bc.invokeInterface(
                    CONFIG_GET_VALUES_MAP,
                    config,
                    Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(DotName.createSimple(keyType))),
                    Const.of(Jandex2Gizmo.classDescOf(DotName.createSimple(valueType))));
        } else if (isCollection(resultType)) {
            Class<?> factoryToUse = DotNames.SET.equals(resultType.name()) ? HashSetFactory.class : ArrayListFactory.class;
            return bc.invokeInterface(
                    CONFIG_GET_VALUES_COLLECTION,
                    config,
                    Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(determineSingleGenericType(resultType, declaringClass))),
                    bc.invokeStatic(MethodDesc.of(factoryToUse, "getInstance", factoryToUse)));
        } else {
            return bc.invokeInterface(
                    CONFIG_GET_VALUE,
                    config, Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(resultType)));
        }
    }

    /**
     * Generates code that uses Config#getOptionalValue for simple objects, or SmallRyeConfig#getOptionalValues if it
     * is a Collection type. Calls the appropriate callback depending on whether the value is present or not.
     *
     * @param propertyName Property name that needs to be fetched
     * @param resultType Type to which the property value needs to be converted to
     * @param declaringClass Config class where the type was encountered
     * @param bc Where the bytecode will be generated
     * @param config Reference to the MP config object
     * @param whenPresent Callback invoked when the optional value is present, receives (blockCreator, unwrappedValue)
     * @param whenAbsent Callback invoked when the optional value is absent, receives blockCreator
     */
    static void createReadOptionalValueAndConvertIfNeeded(
            String propertyName,
            Type resultType,
            DotName declaringClass,
            BlockCreator bc,
            Expr config,
            BiConsumer<BlockCreator, Expr> whenPresent,
            Consumer<BlockCreator> whenAbsent) {

        LocalVar optionalValue;
        if (isMap(resultType)) {
            if (resultType.kind() != Kind.PARAMETERIZED_TYPE) {
                throw new IllegalArgumentException("Unable to resolve Map parameter types for " + propertyName);
            }

            ParameterizedType parameterizedType = resultType.asParameterizedType();
            String keyType = parameterizedType.arguments().get(0).name().toString();
            String valueType = parameterizedType.arguments().get(1).name().toString();

            // Only support Map#value with Converter
            // We don't have a way to validate, so it will fail at runtime (with a proper error message from SR Config)
            optionalValue = bc.localVar("optionalValue", bc.invokeInterface(
                    CONFIG_GET_OPTIONAL_VALUES_MAP,
                    config,
                    Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(DotName.createSimple(keyType))),
                    Const.of(Jandex2Gizmo.classDescOf(DotName.createSimple(valueType)))));
        } else if (isCollection(resultType)) {
            Class<?> factoryToUse = DotNames.SET.equals(resultType.name()) ? HashSetFactory.class : ArrayListFactory.class;
            optionalValue = bc.localVar("optionalValue", bc.invokeInterface(
                    CONFIG_GET_OPTIONAL_VALUES_COLLECTION,
                    config,
                    Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(determineSingleGenericType(resultType, declaringClass))),
                    bc.invokeStatic(MethodDesc.of(factoryToUse, "getInstance", factoryToUse))));
        } else {
            optionalValue = bc.localVar("optionalValue", bc.invokeInterface(
                    CONFIG_GET_OPTIONAL_VALUE,
                    config, Const.of(propertyName),
                    Const.of(Jandex2Gizmo.classDescOf(resultType))));
        }

        Expr isPresent = bc.invokeVirtual(MethodDesc.of(Optional.class, "isPresent", boolean.class), optionalValue);
        bc.ifElse(isPresent, trueBranch -> {
            Expr value = trueBranch.invokeVirtual(MethodDesc.of(Optional.class, "get", Object.class), optionalValue);
            whenPresent.accept(trueBranch, value);
        }, falseBranch -> {
            whenAbsent.accept(falseBranch);
        });
    }

    public static boolean isListOfObject(Type type) {
        if (type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!DotNames.LIST.equals(parameterizedType.name())) {
            return false;
        }
        if (parameterizedType.arguments().size() != 1) {
            return false;
        }
        return !parameterizedType.arguments().get(0).name().toString().startsWith("java");
    }

    private static boolean isCollection(final Type resultType) {
        return DotNames.COLLECTION.equals(resultType.name()) ||
                DotNames.LIST.equals(resultType.name()) ||
                DotNames.SET.equals(resultType.name());
    }

    private static boolean isMap(final Type resultType) {
        return DotNames.MAP.equals(resultType.name()) ||
                DotNames.HASH_MAP.equals(resultType.name());
    }

    static Type determineSingleGenericType(Type type, DotName declaringClass) {
        if (!(type.kind() == Type.Kind.PARAMETERIZED_TYPE)) {
            throw new IllegalArgumentException("Type " + type.name().toString() + " which is used in class " + declaringClass
                    + " must define a generic argument");
        }

        ParameterizedType parameterizedType = type.asParameterizedType();
        if (parameterizedType.arguments().size() != 1) {
            throw new IllegalArgumentException("Type " + type.name().toString() + " which is used in class " + declaringClass
                    + " must define a single generic argument");
        }

        return type.asParameterizedType().arguments().get(0);
    }

    static void registerImplicitConverter(Type type, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        // We need to register for reflection in case an implicit converter is required.
        if (!ConfigBuildStep.isHandledByProducers(type)) {
            if (type.kind() != Type.Kind.ARRAY) {
                reflectiveClasses
                        .produce(ReflectiveClassBuildItem.builder(type.name().toString()).methods().build());
            }
        }
    }
}
