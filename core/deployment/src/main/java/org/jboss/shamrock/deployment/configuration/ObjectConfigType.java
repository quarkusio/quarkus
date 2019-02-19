package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;

import io.smallrye.config.SmallRyeConfig;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import io.quarkus.deployment.AccessorFinder;
import io.quarkus.runtime.configuration.NameIterator;

/**
 */
public class ObjectConfigType extends LeafConfigType {
    final String defaultValue;
    final Class<?> expectedType;

    public ObjectConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment, final String defaultValue, final Class<?> expectedType) {
        super(containingName, container, consumeSegment);
        this.defaultValue = defaultValue;
        this.expectedType = expectedType;
    }

    public Class<?> getItemClass() {
        return expectedType;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.set(enclosing, config.convert(defaultValue, expectedType));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing, final MethodDescriptor setter, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, body.invokeVirtualMethod(SRC_CONVERT_METHOD, config, body.load(defaultValue), body.loadClass(expectedType)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder, final ResultHandle smallRyeConfig) {
        return body.checkCast(body.invokeVirtualMethod(SRC_CONVERT_METHOD, smallRyeConfig, body.load(defaultValue), body.loadClass(expectedType)), expectedType);
    }

    void checkLoaded() {
        if (expectedType == null) throw notLoadedException();
    }

    public void acceptConfigurationValue(final NameIterator name, final SmallRyeConfig config) {
        if (isConsumeSegment()) name.previous();
        getContainer().acceptConfigurationValueIntoLeaf(this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        if (isConsumeSegment()) body.invokeVirtualMethod(NI_PREV_METHOD, name);
        getContainer().generateAcceptConfigurationValueIntoLeaf(body, this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name, final SmallRyeConfig config) {
        try {
            field.set(enclosing, getValue(name, config, expectedType));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing, final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        final ResultHandle optionalValue = body.invokeVirtualMethod(
            SRC_GET_OPT_METHOD,
            config,
            body.invokeVirtualMethod(
                OBJ_TO_STRING_METHOD,
                name
            ),
            body.loadClass(expectedType)
        );
        final ResultHandle defaultValue = body.invokeVirtualMethod(SRC_CONVERT_METHOD, config, body.load(this.defaultValue), body.loadClass(expectedType));
        final ResultHandle value = body.invokeVirtualMethod(OPT_OR_ELSE_METHOD, optionalValue, defaultValue);
        body.invokeStaticMethod(setter, enclosing, value);
    }

    private <T> T getValue(final NameIterator name, final SmallRyeConfig config, Class<T> expectedType) {
        return config.getOptionalValue(name.toString(), expectedType).orElse(config.convert(defaultValue, expectedType));
    }
}
