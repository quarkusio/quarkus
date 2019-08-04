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
public class IntConfigType extends LeafConfigType {
    private static final MethodDescriptor INT_VALUE_METHOD = MethodDescriptor.ofMethod(Integer.class, "intValue", int.class);

    final String defaultValue;
    private final Class<? extends Converter<Integer>> converterClass;

    public IntConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, String javadocKey, String configKey,
            Class<? extends Converter<Integer>> converterClass) {
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
            Integer value = ConfigUtils.getValue(config, name.toString(), Integer.class, converterClass);
            field.setInt(enclosing, value != null ? value.intValue() : 0);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // final Integer integerValue = ConfigUtils.getValue(config, name.toString(), Integer.class, converterClass);
        // final int i = integerValue != null ? integerValue.intValue() : 0;
        final AssignableResultHandle result = body.createVariable(int.class);
        final ResultHandle integerValue = body.checkCast(body.invokeStaticMethod(
                CU_GET_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(Integer.class), loadConverterClass(body)), Integer.class);
        final BranchResult ifNull = body.ifNull(integerValue);
        final BytecodeCreator isNull = ifNull.trueBranch();
        isNull.assign(result, isNull.load(0));
        final BytecodeCreator isNotNull = ifNull.falseBranch();
        isNotNull.assign(result,
                isNotNull.invokeVirtualMethod(
                        INT_VALUE_METHOD,
                        integerValue));
        body.invokeStaticMethod(setter, enclosing, result);
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    @Override
    public Class<?> getItemClass() {
        return int.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            Integer value = ConfigUtils.convert(config, ExpandingConfigSource.expandValue(defaultValue, cache),
                    Integer.class, converterClass);
            field.setInt(enclosing, value != null ? value.intValue() : 0);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing,
                body.invokeVirtualMethod(INT_VALUE_METHOD, getConvertedDefault(body, cache, config)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(INT_VALUE_METHOD, getConvertedDefault(body, cache, smallRyeConfig));
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
                body.loadClass(Integer.class), loadConverterClass(body));
    }

    @Override
    public Class<? extends Converter<Integer>> getConverterClass() {
        return converterClass;
    }
}
