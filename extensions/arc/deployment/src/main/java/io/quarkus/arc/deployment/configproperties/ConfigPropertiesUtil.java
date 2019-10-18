package io.quarkus.arc.deployment.configproperties;

import java.util.Optional;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ArrayListFactory;
import io.quarkus.runtime.configuration.HashSetFactory;
import io.smallrye.config.Converters;
import io.smallrye.config.SmallRyeConfig;

final class ConfigPropertiesUtil {

    static final String PACKAGE_TO_PLACE_GENERATED_CLASSES = "io.quarkus.arc.runtime.config";

    private ConfigPropertiesUtil() {
    }

    /**
     * Generates code that uses config.getValue value to obtain the value of the property.
     * When the result type is a Collection, the string value is obtained from MP config, then
     * split and each token is converted into the required type
     * If the the result type is not a collection, no conversion is performed
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
        EffectiveTypeResponse effectiveTypeResponse = getEffectiveResultType(resultType, declaringClass);

        ResultHandle value = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Config.class, "getValue", Object.class, String.class, Class.class),
                config, bytecodeCreator.load(propertyName),
                bytecodeCreator.loadClass(effectiveTypeResponse.getEffectiveType().toString()));

        return createConversionToFinalValue(resultType.name(),
                effectiveTypeResponse.getEffectiveType(),
                effectiveTypeResponse.getGenericType(), bytecodeCreator, value, config);
    }

    /**
     * Generates code that uses config.getOptional value to obtain the value of the property.
     * When the result type is a Collection, the string value is obtained from MP config, then
     * split and each token is converted into the required type
     * If the the result type is not a collection, no conversion is performed
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
        DotName resultTypeDotName = resultType.name();
        EffectiveTypeResponse effectiveTypeResponse = getEffectiveResultType(resultType, declaringClass);

        ResultHandle optionalValue = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Config.class, "getOptionalValue", Optional.class, String.class,
                        Class.class),
                config, bytecodeCreator.load(propertyName),
                bytecodeCreator.loadClass(effectiveTypeResponse.getEffectiveType().toString()));
        ResultHandle isPresent = bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Optional.class, "isPresent", boolean.class), optionalValue);

        BranchResult isPresentBranch = bytecodeCreator.ifNonZero(isPresent);

        // if the value is present, just call the setter cast to proper type
        BytecodeCreator isPresentTrue = isPresentBranch.trueBranch();
        ResultHandle value = isPresentTrue.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Optional.class, "get", Object.class), optionalValue);

        value = ConfigPropertiesUtil.createConversionToFinalValue(resultTypeDotName,
                effectiveTypeResponse.getEffectiveType(),
                effectiveTypeResponse.getGenericType(),
                isPresentTrue, value, config);

        return new ReadOptionalResponse(value, isPresentTrue, isPresentBranch.falseBranch(), effectiveTypeResponse);
    }

    private static EffectiveTypeResponse getEffectiveResultType(Type resultType, DotName declaringClass) {
        if (DotNames.LIST.equals(resultType.name()) || DotNames.COLLECTION.equals(resultType.name())
                || DotNames.SET.equals(resultType.name())) {
            /*
             * In this case the effective result type which be used to obtain configuration from MP Config will be String
             * we will be converting to the generic type later
             */
            return new EffectiveTypeResponse(DotNames.STRING,
                    ConfigPropertiesUtil.determineSingleGenericType(resultType, declaringClass).name());
        }
        return new EffectiveTypeResponse(resultType.name());
    }

    static ResultHandle createConversionToFinalValue(DotName resultTypeDotName,
            DotName typeUsedToLoadValue,
            DotName genericType,
            BytecodeCreator bytecodeCreator, ResultHandle value, ResultHandle config) {
        if (genericType != null) {
            // TODO make check more generic with other types if/when the need arises
            if (DotNames.LIST.equals(resultTypeDotName) || DotNames.COLLECTION.equals(resultTypeDotName)
                    || DotNames.SET.equals(resultTypeDotName)) {

                // in this case we use io.smallrye.config.Converters#newCollectionConverter

                ResultHandle smallryeConfig = bytecodeCreator.checkCast(config, SmallRyeConfig.class);
                ResultHandle itemConverter = bytecodeCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(SmallRyeConfig.class, "getConverter", Converter.class, Class.class),
                        smallryeConfig, bytecodeCreator.loadClass(genericType.toString()));

                Class<?> factoryToUse = DotNames.SET.equals(resultTypeDotName) ? HashSetFactory.class : ArrayListFactory.class;
                ResultHandle collectionFactory = bytecodeCreator
                        .invokeStaticMethod(MethodDescriptor.ofMethod(factoryToUse, "getInstance", factoryToUse));

                ResultHandle collectionConverter = bytecodeCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Converters.class, "newCollectionConverter", Converter.class, Converter.class,
                                IntFunction.class),
                        itemConverter, collectionFactory);

                return bytecodeCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Converter.class, "convert", Object.class, String.class),
                        collectionConverter, value);
            } else {
                throw new IllegalStateException("Result type " + resultTypeDotName + " is not handled");
            }
        } else if (!resultTypeDotName.equals(typeUsedToLoadValue) && DotNames.STRING.equals(typeUsedToLoadValue)) {
            // in this case we just need to delegate to SmallryeConfig to convert the value for us
            ResultHandle smallryeConfig = bytecodeCreator.checkCast(config, SmallRyeConfig.class);

            return bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(SmallRyeConfig.class, "convert", Object.class, String.class, Class.class),
                    smallryeConfig, value, bytecodeCreator.loadClass(resultTypeDotName.toString()));
        }

        return value;
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

    static class EffectiveTypeResponse {
        private final DotName effectiveType;
        private final DotName genericType;

        public EffectiveTypeResponse(DotName effectiveType) {
            this(effectiveType, null);
        }

        EffectiveTypeResponse(DotName effectiveType, DotName genericType) {
            this.effectiveType = effectiveType;
            this.genericType = genericType;
        }

        public DotName getEffectiveType() {
            return effectiveType;
        }

        public DotName getGenericType() {
            return genericType;
        }
    }

    static class ReadOptionalResponse {
        private final ResultHandle value; //this is only valid within 'isPresentTrue'
        private final BytecodeCreator isPresentTrue;
        private final BytecodeCreator isPresentFalse;
        private final EffectiveTypeResponse effectiveTypeResponse;

        ReadOptionalResponse(ResultHandle value, BytecodeCreator isPresentTrue, BytecodeCreator isPresentFalse,
                EffectiveTypeResponse effectiveTypeResponse) {
            this.value = value;
            this.isPresentTrue = isPresentTrue;
            this.isPresentFalse = isPresentFalse;
            this.effectiveTypeResponse = effectiveTypeResponse;
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

        public EffectiveTypeResponse getEffectiveTypeResponse() {
            return effectiveTypeResponse;
        }
    }
}
