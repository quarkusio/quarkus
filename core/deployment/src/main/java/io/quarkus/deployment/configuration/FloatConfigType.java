package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.Assert;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.deployment.steps.ConfigurationSetup;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class FloatConfigType extends LeafConfigType {

    private static final MethodDescriptor FLOAT_VALUE_METHOD = MethodDescriptor.ofMethod(Float.class, "floatValue",
            float.class);

    final String defaultValue;
    private final Class<? extends Converter<Float>> converterClass;

    public FloatConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, String javadocKey, String configKey, Class<? extends Converter<Float>> converterClass) {
        super(containingName, container, consumeSegment, javadocKey, configKey);
        Assert.checkNotEmptyParam("defaultValue", defaultValue);
        this.defaultValue = defaultValue;
        this.converterClass = converterClass;
    }

    public void acceptConfigurationValue(final NameIterator name, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle cache, final ResultHandle config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            final Float value = ConfigUtils.getValue(config, name.toString(), Float.class, converterClass);
            field.setFloat(enclosing, value != null ? value.floatValue() : 0f);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // final Float floatValue = ConfigUtils.getValue(config, name.toString(), Float.class, converterClass);
        // final float f = floatValue != null ? floatValue.floatValue() : 0f;
        final AssignableResultHandle result = body.createVariable(float.class);
        final ResultHandle floatValue = body.checkCast(body.invokeStaticMethod(
                CU_GET_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(Float.class), loadConverterClass(body)), Float.class);
        final BranchResult ifNull = body.ifNull(floatValue);
        final BytecodeCreator isNull = ifNull.trueBranch();
        isNull.assign(result, isNull.load(0f));
        final BytecodeCreator isNotNull = ifNull.falseBranch();
        isNotNull.assign(result,
                isNotNull.invokeVirtualMethod(
                        FLOAT_VALUE_METHOD,
                        floatValue));
        body.invokeStaticMethod(setter, enclosing, result);
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    @Override
    public Class<?> getItemClass() {
        return float.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            final Float value = ConfigUtils.convert(config, ExpandingConfigSource.expandValue(defaultValue, cache), Float.class,
                    converterClass);
            field.setFloat(enclosing, value != null ? value.floatValue() : 0f);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing,
                body.invokeVirtualMethod(FLOAT_VALUE_METHOD, getConvertedDefault(body, cache, config)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(FLOAT_VALUE_METHOD, getConvertedDefault(body, cache, smallRyeConfig));
    }

    private ResultHandle getConvertedDefault(final BytecodeCreator body, final ResultHandle cache, final ResultHandle config) {
        return body.invokeStaticMethod(
                CU_CONVERT,
                config,
                cache == null ? body.load(defaultValue)
                        : body.invokeStaticMethod(
                                ConfigurationSetup.ECS_EXPAND_VALUE,
                                body.load(defaultValue),
                                cache),
                body.loadClass(Float.class), loadConverterClass(body));
    }

    @Override
    public Class<? extends Converter<Float>> getConverterClass() {
        return converterClass;
    }
}
