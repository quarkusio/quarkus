package org.jboss.shamrock.deployment.configuration;

import java.lang.reflect.Field;

import io.smallrye.config.SmallRyeConfig;
import org.jboss.protean.gizmo.AssignableResultHandle;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.AccessorFinder;
import org.jboss.shamrock.runtime.configuration.NameIterator;
import org.wildfly.common.Assert;

/**
 */
public class FloatConfigType extends LeafConfigType {

    private static final MethodDescriptor FLOAT_VALUE_METHOD = MethodDescriptor.ofMethod(Float.class, "floatValue", float.class);

    final String defaultValue;

    public FloatConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment, final String defaultValue) {
        super(containingName, container, consumeSegment);
        Assert.checkNotEmptyParam("defaultValue", defaultValue);
        this.defaultValue = defaultValue;
    }

    public void acceptConfigurationValue(final NameIterator name, final SmallRyeConfig config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment()) name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        final GroupConfigType container = getContainer(GroupConfigType.class);
        if (isConsumeSegment()) body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name, final SmallRyeConfig config) {
        try {
            final Float floatValue = config.getValue(name.toString(), Float.class);
            final float f = floatValue != null ? floatValue.floatValue() : config.convert(defaultValue, Float.class).floatValue();
            field.setFloat(enclosing, f);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing, final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        // final Float floatValue = config.getValue(name.toString(), Float.class);
        // final float f = floatValue != null ? floatValue.floatValue() : config.convert(defaultValue, Float.class).floatValue();
        final AssignableResultHandle result = body.createVariable(float.class);
        final ResultHandle floatValue = body.checkCast(body.invokeVirtualMethod(
            SRC_GET_VALUE,
            config,
            body.invokeVirtualMethod(
                OBJ_TO_STRING_METHOD,
                name
            ),
            body.loadClass(Float.class)
        ), Float.class);
        final BranchResult ifNull = body.ifNull(floatValue);
        final BytecodeCreator isNull = ifNull.trueBranch();
        isNull.assign(result,
            isNull.checkCast(isNull.invokeVirtualMethod(
                FLOAT_VALUE_METHOD,
                floatValue,
                getConvertedDefault(isNull, config)
            ),
            Float.class)
        );
        final BytecodeCreator isNotNull = ifNull.falseBranch();
        isNotNull.assign(result,
            isNotNull.invokeVirtualMethod(
                FLOAT_VALUE_METHOD,
                floatValue
            )
        );
        body.invokeStaticMethod(setter, enclosing, result);
    }

    public Class<?> getItemClass() {
        return float.class;
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.setFloat(enclosing, config.convert(defaultValue, Float.class).floatValue());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing, final MethodDescriptor setter, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, body.invokeVirtualMethod(FLOAT_VALUE_METHOD, getConvertedDefault(body, config)));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder, final ResultHandle smallRyeConfig) {
        return body.invokeVirtualMethod(FLOAT_VALUE_METHOD, getConvertedDefault(body, smallRyeConfig));
    }

    private ResultHandle getConvertedDefault(final BytecodeCreator body, final ResultHandle config) {
        return body.checkCast(body.invokeVirtualMethod(
            SRC_CONVERT_METHOD,
            config,
            body.load(defaultValue),
            body.loadClass(Float.class)
        ), Float.class);
    }
}
