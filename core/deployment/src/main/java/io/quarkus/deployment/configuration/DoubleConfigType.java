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
public class DoubleConfigType extends LeafConfigType {
    private static final MethodDescriptor DOUBLE_VALUE_METHOD = MethodDescriptor.ofMethod(Double.class, "doubleValue",
            double.class);

    final String defaultValue;
    private final Class<? extends Converter<Double>> converterClass;

    public DoubleConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, String javadocKey, String configKey, Class<? extends Converter<Double>> converterClass) {
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
            Double value = ConfigUtils.getValue(config, name.toString(), Double.class, converterClass);
            field.setDouble(enclosing, value != null ? value.doubleValue() : 0d);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // final Double doubleValue = ConfigUtils.getValue(config, name.toString(), Double.class, converterClass);
        // final double d = doubleValue != null ? doubleValue.doubleValue() : 0d;
        final AssignableResultHandle result = body.createVariable(double.class);
        final ResultHandle doubleValue = body.checkCast(body.invokeStaticMethod(
                CU_GET_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(Double.class), loadConverterClass(body)), Double.class);
        final BranchResult ifNull = body.ifNull(doubleValue);
        final BytecodeCreator isNull = ifNull.trueBranch();
        isNull.assign(result, isNull.load(0d));
        final BytecodeCreator isNotNull = ifNull.falseBranch();
        isNotNull.assign(result,
                isNotNull.invokeVirtualMethod(
                        DOUBLE_VALUE_METHOD,
                        doubleValue));
        body.invokeStaticMethod(setter, enclosing, result);
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    @Override
    public Class<?> getItemClass() {
        return double.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            Double value = ConfigUtils.convert(config,
                    ExpandingConfigSource.expandValue(defaultValue, cache), Double.class, converterClass);
            field.setDouble(enclosing, value != null ? value.doubleValue() : 0d);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing,
                body.invokeVirtualMethod(DOUBLE_VALUE_METHOD, getConvertedDefault(body, cache, config)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(DOUBLE_VALUE_METHOD, getConvertedDefault(body, cache, smallRyeConfig));
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
                body.loadClass(Double.class), loadConverterClass(body));
    }

    @Override
    public Class<? extends Converter<Double>> getConverterClass() {
        return converterClass;
    }
}
