package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.OptionalInt;

import org.wildfly.common.Assert;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class IntConfigType extends LeafConfigType {

    private static final MethodDescriptor OPTINT_OR_ELSE_METHOD = MethodDescriptor.ofMethod(OptionalInt.class, "orElse",
            int.class, int.class);
    private static final MethodDescriptor INT_VALUE_METHOD = MethodDescriptor.ofMethod(Integer.class, "intValue", int.class);

    final String defaultValue;

    public IntConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue) {
        super(containingName, container, consumeSegment);
        Assert.checkNotEmptyParam("defaultValue", defaultValue);
        this.defaultValue = defaultValue;
    }

    public void acceptConfigurationValue(final NameIterator name, final SmallRyeConfig config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.setInt(enclosing, config.getValue(name.toString(), OptionalInt.class)
                    .orElse(config.convert(defaultValue, Integer.class).intValue()));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // config.getValue(name.toString(), OptionalInt.class).orElse(config.convert(defaultValue, Integer.class).intValue())
        final ResultHandle optionalValue = body.checkCast(body.invokeVirtualMethod(
                SRC_GET_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(OptionalInt.class)), OptionalInt.class);
        final ResultHandle convertedDefault = getConvertedDefault(body, config);
        final ResultHandle defaultedValue = body.checkCast(body.invokeVirtualMethod(
                OPTINT_OR_ELSE_METHOD,
                optionalValue,
                convertedDefault), Integer.class);
        final ResultHandle intValue = body.invokeVirtualMethod(INT_VALUE_METHOD, defaultedValue);
        body.invokeStaticMethod(setter, enclosing, intValue);
    }

    public String getDefaultValueString() {
        return defaultValue;
    }

    public Class<?> getItemClass() {
        return int.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.setInt(enclosing, config.convert(defaultValue, Integer.class).intValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing,
                body.invokeVirtualMethod(INT_VALUE_METHOD, getConvertedDefault(body, config)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(INT_VALUE_METHOD, getConvertedDefault(body, smallRyeConfig));
    }

    private ResultHandle getConvertedDefault(final BytecodeCreator body, final ResultHandle config) {
        return body.checkCast(body.invokeVirtualMethod(
                SRC_CONVERT_METHOD,
                config,
                body.load(defaultValue),
                body.loadClass(Integer.class)), Integer.class);
    }
}
