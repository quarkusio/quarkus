package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class MapConfigType extends CompoundConfigType {

    private static final MethodDescriptor TREE_MAP_CTOR = MethodDescriptor.ofConstructor(TreeMap.class);
    private static final MethodDescriptor MAP_GET_METHOD = MethodDescriptor.ofMethod(Map.class, "get", Object.class,
            Object.class);
    private static final MethodDescriptor MAP_PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class,
            Object.class, Object.class);

    public MapConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment) {
        super(containingName, container, consumeSegment);
    }

    public void load() {
    }

    @SuppressWarnings("unchecked")
    Object getChildObject(final NameIterator name, final ExpandingConfigSource.Cache cache, final SmallRyeConfig config,
            final Object self, final String childName) {
        return ((TreeMap<String, Object>) self).get(name.getNextSegment());
    }

    ResultHandle generateGetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle cache,
            final ResultHandle config,
            final ResultHandle self, final String childName) {
        return body.invokeInterfaceMethod(MAP_GET_METHOD, body.checkCast(self, Map.class),
                body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name));
    }

    @SuppressWarnings("unchecked")
    void setChildObject(final NameIterator name, final Object self, final String childName, final Object value) {
        ((TreeMap<String, Object>) self).put(name.getNextSegment(), value);
    }

    void generateSetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle self,
            final String containingName, final ResultHandle value) {
        body.invokeInterfaceMethod(MAP_PUT_METHOD, body.checkCast(self, Map.class),
                body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name), value);
    }

    TreeMap<String, Object> getOrCreate(final NameIterator name, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        TreeMap<String, Object> self;
        if (container != null) {
            if (isConsumeSegment())
                name.previous();
            final Object enclosing = container.getOrCreate(name, cache, config);
            self = (TreeMap<String, Object>) container.getChildObject(name, cache, config, enclosing, getContainingName());
            if (self == null) {
                self = new TreeMap<>();
                container.setChildObject(name, enclosing, getContainingName(), self);
            }
            if (isConsumeSegment())
                name.next();
        } else {
            self = new TreeMap<>();
        }
        return self;
    }

    ResultHandle generateGetOrCreate(final BytecodeCreator body, final ResultHandle name, final ResultHandle cache,
            final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (container != null) {
            if (isConsumeSegment())
                body.invokeVirtualMethod(NI_PREV_METHOD, name);
            final ResultHandle enclosing = container.generateGetOrCreate(body, name, cache, config);
            final AssignableResultHandle self = body.createVariable(TreeMap.class);
            body.assign(self, body.checkCast(
                    container.generateGetChildObject(body, name, cache, config, enclosing, getContainingName()), Map.class));
            try (BytecodeCreator selfIsNull = body.ifNull(self).trueBranch()) {
                selfIsNull.assign(self, selfIsNull.newInstance(TREE_MAP_CTOR));
                container.generateSetChildObject(selfIsNull, name, enclosing, getContainingName(), self);
            }
            if (isConsumeSegment())
                body.invokeVirtualMethod(NI_NEXT_METHOD, name);
            return self;
        } else {
            return body.newInstance(TREE_MAP_CTOR);
        }
    }

    void acceptConfigurationValueIntoLeaf(final LeafConfigType leafType, final NameIterator name,
            final ExpandingConfigSource.Cache cache, final SmallRyeConfig config) {
        leafType.acceptConfigurationValueIntoMap(getOrCreate(name, cache, config), name, config);
    }

    void generateAcceptConfigurationValueIntoLeaf(final BytecodeCreator body, final LeafConfigType leafType,
            final ResultHandle name, final ResultHandle cache, final ResultHandle config) {
        leafType.generateAcceptConfigurationValueIntoMap(body, generateGetOrCreate(body, name, cache, config), name, config);
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig) {
        return body.newInstance(TREE_MAP_CTOR);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field) {
        try {
            field.set(enclosing, new TreeMap<>());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, body.newInstance(TREE_MAP_CTOR));
    }
}
