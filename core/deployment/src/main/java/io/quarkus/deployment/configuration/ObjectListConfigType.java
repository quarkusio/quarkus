package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ArrayListFactory;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class ObjectListConfigType extends ObjectConfigType {

    static final MethodDescriptor ALF_GET_INST_METHOD = MethodDescriptor.ofMethod(ArrayListFactory.class, "getInstance",
            ArrayListFactory.class);
    static final MethodDescriptor EMPTY_LIST_METHOD = MethodDescriptor.ofMethod(Collections.class, "emptyList", List.class);
    static final MethodDescriptor CU_GET_DEFAULTS_METHOD = MethodDescriptor.ofMethod(ConfigUtils.class, "getDefaults",
            Collection.class, SmallRyeConfig.class, String.class, Class.class, IntFunction.class);

    public ObjectListConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, final Class<?> expectedType) {
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
            field.set(enclosing, defaultValue.isEmpty() ? Collections.emptyList()
                    : ConfigUtils.getDefaults(config, defaultValue, expectedType, ArrayListFactory.getInstance()));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        final ResultHandle value;
        if (defaultValue.isEmpty()) {
            value = body.invokeStaticMethod(EMPTY_LIST_METHOD);
        } else {
            value = body.invokeStaticMethod(CU_GET_DEFAULTS_METHOD, config, body.load(defaultValue),
                    body.loadClass(expectedType), body.invokeStaticMethod(ALF_GET_INST_METHOD));
        }
        body.invokeStaticMethod(setter, enclosing, value);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.set(enclosing, getValues(name, config));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    public void generateAcceptConfigurationValueIntoGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle name, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, generateGetValues(body, name, config));
    }

    void acceptConfigurationValueIntoMap(final Map<String, Object> enclosing, final NameIterator name,
            final SmallRyeConfig config) {
        enclosing.put(name.getNextSegment(), getValues(name, config));
    }

    void generateAcceptConfigurationValueIntoMap(final BytecodeCreator body, final ResultHandle enclosing,
            final ResultHandle name, final ResultHandle config) {
        body.invokeInterfaceMethod(MAP_PUT_METHOD, enclosing, body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name),
                generateGetValues(body, name, config));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle config) {
        return body.checkCast(body.invokeStaticMethod(CU_GET_DEFAULTS_METHOD, config, body.load(defaultValue),
                body.loadClass(expectedType), body.invokeStaticMethod(ALF_GET_INST_METHOD)), List.class);
    }

    private ArrayList<?> getValues(final NameIterator name, final SmallRyeConfig config) {
        return config.getValues(name.toString(), expectedType, ArrayListFactory.getInstance());
    }

    private ResultHandle generateGetValues(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        return body.invokeVirtualMethod(
                SRC_GET_VALUES_METHOD,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(expectedType),
                body.invokeStaticMethod(ALF_GET_INST_METHOD));
    }
}
