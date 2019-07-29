package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.steps.ConfigurationSetup.ECS_EXPAND_VALUE;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.microprofile.config.spi.Converter;

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
public class ObjectConfigType<T> extends LeafConfigType {
    final String defaultValue;
    final Class<T> expectedType;
    Class<? extends Converter<T>> converterClass;

    public ObjectConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, final Class<T> expectedType, String javadocKey, String configKey,
            Class<? extends Converter<T>> converterClass) {
        super(containingName, container, consumeSegment, javadocKey, configKey);
        this.defaultValue = defaultValue;
        this.expectedType = expectedType;
        this.converterClass = converterClass;
    }

    @Override
    public Class<T> getItemClass() {
        return expectedType;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            String value = ExpandingConfigSource.expandValue(defaultValue, cache);
            field.set(enclosing, ConfigUtils.convert(config, value, expectedType, converterClass));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        ResultHandle resultHandle = getResultHandle(body, cache, config);
        body.invokeStaticMethod(setter, enclosing, resultHandle);
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        ResultHandle resultHandle = getResultHandle(body, cache, smallRyeConfig);
        return body.checkCast(resultHandle, expectedType);
    }

    private ResultHandle getResultHandle(BytecodeCreator body, ResultHandle cache, ResultHandle smallRyeConfig) {
        ResultHandle clazz = body.loadClass(expectedType);
        ResultHandle cacheResultHandle = cache == null ? body.load(defaultValue)
                : body.invokeStaticMethod(ECS_EXPAND_VALUE,
                        body.load(defaultValue),
                        cache);

        return body.invokeStaticMethod(CU_CONVERT, smallRyeConfig, cacheResultHandle, clazz, loadConverterClass(body));
    }

    public void acceptConfigurationValue(final NameIterator name, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config) {
        if (isConsumeSegment())
            name.previous();
        getContainer().acceptConfigurationValueIntoLeaf(this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle cache, final ResultHandle config) {
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        getContainer().generateAcceptConfigurationValueIntoLeaf(body, this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.set(enclosing,
                    ConfigUtils.getOptionalValue(config, name.toString(), expectedType, converterClass)
                            .orElse(null));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, generateGetValue(body, name, config));
    }

    void acceptConfigurationValueIntoMap(final Map<String, Object> enclosing, final NameIterator name,
            final SmallRyeConfig config) {
        enclosing.put(name.getNextSegment(),
                ConfigUtils.getOptionalValue(config, name.toString(), expectedType, converterClass).orElse(null));
    }

    void generateAcceptConfigurationValueIntoMap(final BytecodeCreator body, final ResultHandle enclosing,
            final ResultHandle name, final ResultHandle config) {
        body.invokeInterfaceMethod(MAP_PUT_METHOD, enclosing, body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name),
                generateGetValue(body, name, config));
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    private ResultHandle generateGetValue(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        final ResultHandle optionalValue = body.invokeStaticMethod(
                CU_GET_OPT_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(expectedType), loadConverterClass(body));
        return body.invokeVirtualMethod(OPT_OR_ELSE_METHOD, optionalValue, body.loadNull());
    }

    @Override
    public Class<? extends Converter<T>> getConverterClass() {
        return converterClass;
    }
}
