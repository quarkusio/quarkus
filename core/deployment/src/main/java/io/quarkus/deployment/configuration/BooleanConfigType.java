package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Optional;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class BooleanConfigType extends LeafConfigType {
    private static final MethodDescriptor BOOL_VALUE_METHOD = MethodDescriptor.ofMethod(Boolean.class, "booleanValue",
            boolean.class);

    final String defaultValue;

    public BooleanConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue) {
        super(containingName, container, consumeSegment);
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
            field.setBoolean(enclosing, config.getOptionalValue(name.toString(), Boolean.class)
                    .orElse(config.convert(defaultValue, Boolean.class)).booleanValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // config.getOptionalValue(name.toString(), Boolean.class).orElse(config.convert(defaultValue, Boolean.class)).booleanValue()
        final ResultHandle optionalValue = body.checkCast(body.invokeVirtualMethod(
                SRC_GET_OPT_METHOD,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(Boolean.class)), Optional.class);
        final ResultHandle convertedDefault = getConvertedDefault(body, config);
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

    public Class<?> getItemClass() {
        return boolean.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.setBoolean(enclosing, config.convert(defaultValue, Boolean.class).booleanValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        final ResultHandle value = body.invokeVirtualMethod(BOOL_VALUE_METHOD,
                body.checkCast(getConvertedDefault(body, config), Boolean.class));
        body.invokeStaticMethod(setter, enclosing, value);
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(BOOL_VALUE_METHOD,
                body.checkCast(getConvertedDefault(body, smallRyeConfig), Boolean.class));
    }

    private ResultHandle getConvertedDefault(final BytecodeCreator body, final ResultHandle config) {
        return body.checkCast(body.invokeVirtualMethod(
                SRC_CONVERT_METHOD,
                config,
                body.load(defaultValue),
                body.loadClass(Boolean.class)), Boolean.class);
    }
}
