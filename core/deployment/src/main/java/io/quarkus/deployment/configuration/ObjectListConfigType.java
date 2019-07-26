package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.steps.ConfigurationSetup.ECS_EXPAND_VALUE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ArrayListFactory;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class ObjectListConfigType<T> extends ObjectConfigType<T> {
    static final MethodDescriptor ALF_GET_INST_METHOD = MethodDescriptor.ofMethod(ArrayListFactory.class, "getInstance",
            ArrayListFactory.class);
    static final MethodDescriptor EMPTY_LIST_METHOD = MethodDescriptor.ofMethod(Collections.class, "emptyList", List.class);
    static final MethodDescriptor CU_GET_DEFAULTS_METHOD = MethodDescriptor.ofMethod(ConfigUtils.class, "getDefaults",
            Collection.class, SmallRyeConfig.class, String.class, Class.class, Class.class, IntFunction.class);

    static final MethodDescriptor GET_VALUES = MethodDescriptor.ofMethod(ConfigUtils.class, "getValues", ArrayList.class,
            SmallRyeConfig.class, String.class, Class.class, Class.class);

    public ObjectListConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final String defaultValue, final Class<T> expectedType, String javadocKey, String configKey,
            Class<? extends Converter<T>> converterClass) {
        super(containingName, container, consumeSegment, defaultValue, expectedType, javadocKey, configKey, converterClass);
    }

    public void acceptConfigurationValue(final NameIterator name, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    public void generateAcceptConfigurationValue(final BytecodeCreator body, final ResultHandle name,
            final ResultHandle cache, final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        container.generateAcceptConfigurationValueIntoLeaf(body, this, name, cache, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            if (defaultValue.isEmpty()) {
                field.set(enclosing, Collections.emptyList());
            } else {
                final ArrayList<?> defaults = ConfigUtils.getDefaults(
                        config,
                        ExpandingConfigSource.expandValue(defaultValue, cache),
                        expectedType,
                        converterClass,
                        ArrayListFactory.getInstance());
                field.set(enclosing, defaults);
            }
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        final ResultHandle value;
        if (defaultValue.isEmpty()) {
            value = body.invokeStaticMethod(EMPTY_LIST_METHOD);
        } else {
            ResultHandle cacheValue = cache == null ? body.load(defaultValue)
                    : body.invokeStaticMethod(ECS_EXPAND_VALUE,
                            body.load(defaultValue),
                            cache);
            value = body.invokeStaticMethod(CU_GET_DEFAULTS_METHOD, config, cacheValue, body.loadClass(expectedType),
                    loadConverterClass(body),
                    body.invokeStaticMethod(ALF_GET_INST_METHOD));
        }
        body.invokeStaticMethod(setter, enclosing, value);
    }

    public void acceptConfigurationValueIntoGroup(final Object enclosing, final Field field, final NameIterator name,
            final SmallRyeConfig config) {
        try {
            field.set(enclosing, ConfigUtils.getValues(config, name.toString(), expectedType, converterClass));
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
        enclosing.put(name.getNextSegment(),
                ConfigUtils.getValues(config, name.toString(), expectedType, converterClass));
    }

    void generateAcceptConfigurationValueIntoMap(final BytecodeCreator body, final ResultHandle enclosing,
            final ResultHandle name, final ResultHandle config) {
        body.invokeInterfaceMethod(MAP_PUT_METHOD, enclosing, body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name),
                generateGetValues(body, name, config));
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle config) {
        ResultHandle arrayListFactory = body.invokeStaticMethod(ALF_GET_INST_METHOD);
        final ResultHandle resultHandle = body.invokeStaticMethod(CU_GET_DEFAULTS_METHOD, config, body.load(defaultValue),
                body.loadClass(expectedType), loadConverterClass(body), arrayListFactory);
        return body.checkCast(resultHandle, List.class);
    }

    private ResultHandle generateGetValues(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        ResultHandle propertyName = body.invokeVirtualMethod(OBJ_TO_STRING_METHOD, name);
        return body.invokeStaticMethod(GET_VALUES, config, propertyName, body.loadClass(expectedType),
                loadConverterClass(body));
    }
}
