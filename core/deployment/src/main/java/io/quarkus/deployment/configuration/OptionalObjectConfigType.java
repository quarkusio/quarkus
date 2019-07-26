package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.steps.ConfigurationSetup.ECS_EXPAND_VALUE;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.Assert;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class OptionalObjectConfigType<T> extends ObjectConfigType<T> {

    public OptionalObjectConfigType(final String containingName, final CompoundConfigType container,
            final boolean consumeSegment, final String defaultValue, final Class<T> expectedType, String javadocKey,
            String configKey, Class<? extends Converter<T>> converterClass) {
        super(containingName, container, consumeSegment, defaultValue, expectedType, javadocKey, configKey, converterClass);
    }

    public void acceptConfigurationValue(final NameIterator name, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle cache, final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            if (defaultValue.isEmpty()) {
                field.set(enclosing, Optional.empty());
            } else {
                String value = ExpandingConfigSource.expandValue(defaultValue, cache);
                field.set(enclosing,
                        Optional.ofNullable(ConfigUtils.convert(config, value, expectedType, converterClass)));
            }
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        final ResultHandle optValue;
        if (defaultValue.isEmpty()) {
            optValue = body.invokeStaticMethod(OPT_EMPTY_METHOD);
        } else {
            optValue = body.invokeStaticMethod(OPT_OF_NULLABLE_METHOD, body.invokeStaticMethod(CU_CONVERT, config,
                    body.load(defaultValue), body.loadClass(expectedType), loadConverterClass(body)));
        }
        body.invokeStaticMethod(setter, enclosing, optValue);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.set(enclosing, ConfigUtils.getOptionalValue(config, name.toString(), expectedType, converterClass));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        ResultHandle propertyName = body.invokeVirtualMethod(OBJ_TO_STRING_METHOD, name);
        final ResultHandle optionalValue = body.invokeStaticMethod(CU_GET_OPT_VALUE, config, propertyName,
                body.loadClass(expectedType), loadConverterClass(body));
        body.invokeStaticMethod(setter, enclosing, optionalValue);
    }

    void acceptConfigurationValueIntoMap(final Map<String, Object> enclosing, final NameIterator name,
            final SmallRyeConfig config) {
        throw Assert.unsupported();
    }

    void generateAcceptConfigurationValueIntoMap(final BytecodeCreator body, final ResultHandle enclosing,
            final ResultHandle name, final ResultHandle config) {
        throw Assert.unsupported();
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle config) {
        if (defaultValue.isEmpty()) {
            return body.invokeStaticMethod(OPT_EMPTY_METHOD);
        } else {
            ResultHandle classResultHandle = body.loadClass(expectedType);
            ResultHandle cacheResultHandle = cache == null ? body.load(defaultValue)
                    : body.invokeStaticMethod(ECS_EXPAND_VALUE,
                            body.load(defaultValue),
                            cache);
            ResultHandle resultHandle = body.invokeStaticMethod(CU_CONVERT, config, cacheResultHandle, classResultHandle,
                    loadConverterClass(body));
            return body.invokeStaticMethod(OPT_OF_NULLABLE_METHOD, resultHandle);
        }
    }
}
