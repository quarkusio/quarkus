package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.deployment.steps.ConfigurationSetup;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class BooleanConfigType extends LeafConfigType {
    private static final MethodDescriptor BOOL_VALUE_METHOD = MethodDescriptor.ofMethod(Boolean.class, "booleanValue",
            boolean.class);

    final String defaultValue;
    private final Class<? extends Converter<Boolean>> converterClass;

    public BooleanConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, String javadocKey, String configKey,
            Class<? extends Converter<Boolean>> converterClass) {
        super(containingName, container, consumeSegment, javadocKey, configKey);
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
            Optional<Boolean> optionalValue = ConfigUtils.getOptionalValue(config, name.toString(), Boolean.class,
                    converterClass);
            field.setBoolean(enclosing, optionalValue.orElse(Boolean.FALSE).booleanValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // ConfigUtils.getOptionalValue(config, name.toString(), Boolean.class, converterClass).orElse(Boolean.FALSE).booleanValue()
        final ResultHandle optionalValue = body.checkCast(body.invokeStaticMethod(
                CU_GET_OPT_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(Boolean.class), loadConverterClass(body)), Optional.class);
        final ResultHandle convertedDefault = body.readStaticField(FieldDescriptor.of(Boolean.class, "FALSE", Boolean.class));
        final ResultHandle defaultedValue = body.checkCast(body.invokeVirtualMethod(
                OPT_OR_ELSE_METHOD,
                optionalValue,
                convertedDefault), Boolean.class);
        final ResultHandle booleanValue = body.invokeVirtualMethod(BOOL_VALUE_METHOD, defaultedValue);
        body.invokeStaticMethod(setter, enclosing, booleanValue);
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    @Override
    public Class<? extends Converter<Boolean>> getConverterClass() {
        return converterClass;
    }

    @Override
    public Class<?> getItemClass() {
        return boolean.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            Boolean value = ConfigUtils.convert(config, ExpandingConfigSource.expandValue(defaultValue, cache), Boolean.class,
                    converterClass);
            field.setBoolean(enclosing, value.booleanValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        final ResultHandle value = body.invokeVirtualMethod(BOOL_VALUE_METHOD, getConvertedDefault(body, cache, config));
        body.invokeStaticMethod(setter, enclosing, value);
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(BOOL_VALUE_METHOD, getConvertedDefault(body, cache, smallRyeConfig));
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
                body.loadClass(Boolean.class), loadConverterClass(body));
    }
}
