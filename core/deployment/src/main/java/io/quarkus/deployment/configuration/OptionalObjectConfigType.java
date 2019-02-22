package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Optional;

import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class OptionalObjectConfigType extends ObjectConfigType {

    public OptionalObjectConfigType(final String containingName, final CompoundConfigType container,
            final boolean consumeSegment, final String defaultValue, final Class<?> expectedType) {
        super(containingName, container, consumeSegment, defaultValue, expectedType);
    }

    public void acceptConfigurationValue(final NameIterator name, final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            if (defaultValue.isEmpty()) {
                field.set(enclosing, Optional.empty());
            } else {
                field.set(enclosing, Optional.ofNullable(config.convert(defaultValue, expectedType)));
            }
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        ResultHandle optValue;
        if (defaultValue.isEmpty()) {
            optValue = body.invokeStaticMethod(OPT_EMPTY_METHOD);
        } else {
            optValue = body.invokeStaticMethod(OPT_OF_NULLABLE_METHOD, body.invokeVirtualMethod(SRC_CONVERT_METHOD, config,
                    body.load(defaultValue), body.loadClass(expectedType)));
        }
        body.invokeStaticMethod(setter, enclosing, optValue);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.set(enclosing, config.getOptionalValue(name.toString(), expectedType));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        final ResultHandle optionalValue = body.invokeVirtualMethod(
                SRC_GET_OPT_METHOD,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(expectedType));
        body.invokeStaticMethod(setter, enclosing, optionalValue);
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle config) {
        if (defaultValue.isEmpty()) {
            return body.invokeStaticMethod(OPT_EMPTY_METHOD);
        } else {
            return body.invokeStaticMethod(OPT_OF_NULLABLE_METHOD, body.invokeVirtualMethod(SRC_CONVERT_METHOD, config,
                    body.load(defaultValue), body.loadClass(expectedType)));
        }
    }
}
