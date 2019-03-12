package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Map;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 * Handle Map<String,String> leaf values
 */
public class MapValueConfigType extends LeafConfigType {
    final Class<?> expectedType;

    public MapValueConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
                            final Class<?> expectedType) {
        super(containingName, container, consumeSegment);
        this.expectedType = expectedType;
    }

    @Override
    public Class<?> getItemClass() {
        return expectedType;
    }

    @Override
    public void acceptConfigurationValue(NameIterator name, SmallRyeConfig config) {
        // Expect to be in a MapConfigType contained in a GroupConfigType
        final MapConfigType mapContainer = getContainer(MapConfigType.class);
        final GroupConfigType container = mapContainer.getContainer(GroupConfigType.class);
        if (isConsumeSegment())
            name.previous();
        container.acceptConfigurationValueIntoLeaf(this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) name.next();
    }

    @Override
    public void generateAcceptConfigurationValue(BytecodeCreator body, ResultHandle name, ResultHandle config) {
        // Expect to be in a MapConfigType contained in a GroupConfigType
        final MapConfigType mapContainer = getContainer(MapConfigType.class);
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        mapContainer.generateAcceptConfigurationValueIntoLeaf(body, this, name, config);
        // the iterator is not used after this point
        // if (isConsumeSegment()) body.invokeVirtualMethod(NI_NEXT_METHOD, name);
    }

    @Override
    void acceptConfigurationValueIntoGroup(Object enclosing, Field field, NameIterator name, SmallRyeConfig config) {
        try {
            Map map = (Map) field.get(enclosing);
            String value = config.getValue(name.toString(), String.class);
            String key = name.getNextSegment();
            map.put(key, value);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }

    }

    @Override
    void generateAcceptConfigurationValueIntoGroup(BytecodeCreator body, ResultHandle enclosing, MethodDescriptor setter, ResultHandle name, ResultHandle config) {
        // Expect to be in a MapConfigType contained in a GroupConfigType
        final MapConfigType mapContainer = getContainer(MapConfigType.class);
        final GroupConfigType container = mapContainer.getContainer(GroupConfigType.class);
        // config.getValue(name.toString(), String.class)
        final ResultHandle value = body.checkCast(body.invokeVirtualMethod(
                SRC_GET_VALUE,
                config,
                body.invokeVirtualMethod(
                        OBJ_TO_STRING_METHOD,
                        name),
                body.loadClass(String.class)), String.class);
        final ResultHandle key = body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name);
        body.invokeStaticMethod(setter, enclosing, key, value);
    }

    @Override
    void getDefaultValueIntoEnclosingGroup(Object enclosing, SmallRyeConfig config, Field field) {
        // noop
    }

    @Override
    void generateGetDefaultValueIntoEnclosingGroup(BytecodeCreator body, ResultHandle enclosing, MethodDescriptor setter, ResultHandle config) {
        // noop
    }

    @Override
    public ResultHandle writeInitialization(BytecodeCreator body, AccessorFinder accessorFinder, ResultHandle smallRyeConfig) {
        return null;
    }
}
