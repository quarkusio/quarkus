package io.quarkus.arc.deployment.configproperties;

import java.util.Collection;
import java.util.Optional;
import java.util.function.IntFunction;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.ConfigBuildStep;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ArrayListFactory;
import io.quarkus.runtime.configuration.HashSetFactory;
import io.smallrye.config.SmallRyeConfig;

final class ConfigPropertiesUtil {

    static final String PACKAGE_TO_PLACE_GENERATED_CLASSES = "io.quarkus.arc.runtime.config";

    private ConfigPropertiesUtil() {
    }

    /**
     * Generates code that uses Config#getValue for simple objects, or SmallRyeConfig#getValues if it is a Collection
     * type.
     *
     * @param propertyName Property name that needs to be fetched
     * @param resultType Type to which the property value needs to be converted to
     * @param declaringClass Config class where the type was encountered
     * @param bytecodeCreator Where the bytecode will be generated
     * @param config Reference to the MP config object
     */
    static ResultHandle createReadMandatoryValueAndConvertIfNeeded(String propertyName,
            Type resultType,
            DotName declaringClass,
            BytecodeCreator bytecodeCreator, ResultHandle config) {

        if (isMap(resultType)) {
            throw new DeploymentException(
                    "Using a Map is not supported for classes annotated with '@ConfigProperties'. Consider using https://quarkus.io/guides/config-mappings instead.");
        }
        if (isCollection(resultType)) {
            ResultHandle smallryeConfig = bytecodeCreator.checkCast(config, SmallRyeConfig.class);

            Class<?> factoryToUse = DotNames.SET.equals(resultType.name()) ? HashSetFactory.class : ArrayListFactory.class;
            ResultHandle collectionFactory = bytecodeCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(factoryToUse, "getInstance", factoryToUse));

            return bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValues", Collection.class, String.class,
                            Class.class, IntFunction.class),
                    smallryeConfig,
                    bytecodeCreator.load(propertyName),
                    bytecodeCreator.loadClass(determineSingleGenericType(resultType, declaringClass).name().toString()),
                    collectionFactory);
        } else {
            return bytecodeCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Config.class, "getValue", Object.class, String.class, Class.class),
                    config, bytecodeCreator.load(propertyName),
                    bytecodeCreator.loadClass(resultType.name().toString()));
        }
    }

    /**
     * Generates code that uses Config#getOptionalValue for simple objects, or SmallRyeConfig#getOptionalValues if it
     * is a Collection type.
     *
     * @param propertyName Property name that needs to be fetched
     * @param resultType Type to which the property value needs to be converted to
     * @param declaringClass Config class where the type was encountered
     * @param bytecodeCreator Where the bytecode will be generated
     * @param config Reference to the MP config object
     */
    static ReadOptionalResponse createReadOptionalValueAndConvertIfNeeded(String propertyName, Type resultType,
            DotName declaringClass,
            BytecodeCreator bytecodeCreator, ResultHandle config) {

        ResultHandle optionalValue;
        if (isMap(resultType)) {
            throw new DeploymentException(
                    "Using a Map is not supported for classes annotated with '@ConfigProperties'. Consider using https://quarkus.io/guides/config-mappings instead.");
        }
        if (isCollection(resultType)) {
            ResultHandle smallryeConfig = bytecodeCreator.checkCast(config, SmallRyeConfig.class);

            Class<?> factoryToUse = DotNames.SET.equals(resultType.name()) ? HashSetFactory.class : ArrayListFactory.class;
            ResultHandle collectionFactory = bytecodeCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(factoryToUse, "getInstance", factoryToUse));

            optionalValue = bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(SmallRyeConfig.class, "getOptionalValues", Optional.class, String.class,
                            Class.class, IntFunction.class),
                    smallryeConfig,
                    bytecodeCreator.load(propertyName),
                    bytecodeCreator.loadClass(determineSingleGenericType(resultType, declaringClass).name().toString()),
                    collectionFactory);
        } else {
            optionalValue = bytecodeCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Config.class, "getOptionalValue", Optional.class, String.class,
                            Class.class),
                    config, bytecodeCreator.load(propertyName),
                    bytecodeCreator.loadClass(resultType.name().toString()));
        }

        ResultHandle isPresent = bytecodeCreator
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Optional.class, "isPresent", boolean.class), optionalValue);
        BranchResult isPresentBranch = bytecodeCreator.ifNonZero(isPresent);
        BytecodeCreator isPresentTrue = isPresentBranch.trueBranch();
        ResultHandle value = isPresentTrue.invokeVirtualMethod(MethodDescriptor.ofMethod(Optional.class, "get", Object.class),
                optionalValue);
        return new ReadOptionalResponse(value, isPresentTrue, isPresentBranch.falseBranch());
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
                reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, type.name().toString()));
            }
        }
    }

    static class ReadOptionalResponse {
        private final ResultHandle value; //this is only valid within 'isPresentTrue'
        private final BytecodeCreator isPresentTrue;
        private final BytecodeCreator isPresentFalse;

        ReadOptionalResponse(ResultHandle value, BytecodeCreator isPresentTrue, BytecodeCreator isPresentFalse) {
            this.value = value;
            this.isPresentTrue = isPresentTrue;
            this.isPresentFalse = isPresentFalse;
        }

        public ResultHandle getValue() {
            return value;
        }

        public BytecodeCreator getIsPresentTrue() {
            return isPresentTrue;
        }

        public BytecodeCreator getIsPresentFalse() {
            return isPresentFalse;
        }
    }
}
