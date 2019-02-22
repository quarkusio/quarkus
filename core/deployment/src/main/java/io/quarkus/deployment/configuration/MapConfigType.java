package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.protean.gizmo.AssignableResultHandle;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.wildfly.common.Assert;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public class MapConfigType extends CompoundConfigType {

    private static final MethodDescriptor TREE_MAP_CTOR = MethodDescriptor.ofConstructor(TreeMap.class);

    public MapConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment) {
        super(containingName, container, consumeSegment);
    }

    public void load() throws ClassNotFoundException {
    }

    @SuppressWarnings("unchecked")
    Object getChildObject(final NameIterator name, final SmallRyeConfig config, final Object self, final String childName) {
        return ((TreeMap<String, Object>) self).get(childName);
    }

    ResultHandle generateGetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle config,
            final ResultHandle self, final String childName) {
        return body.invokeVirtualMethod(MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class),
                body.checkCast(self, Map.class), body.load(childName));
    }

    @SuppressWarnings("unchecked")
    void setChildObject(final NameIterator name, final Object self, final String childName, final Object value) {
        ((TreeMap<String, Object>) self).put(name.getNextSegment(), value);
    }

    void generateSetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle self,
            final String containingName, final ResultHandle value) {
        body.invokeVirtualMethod(MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                body.checkCast(self, Map.class), body.invokeVirtualMethod(NI_GET_NEXT_SEGMENT, name), value);
    }

    TreeMap<String, Object> getOrCreate(final NameIterator name, final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        TreeMap<String, Object> self;
        if (container != null) {
            if (isConsumeSegment())
                name.previous();
            final Object enclosing = container.getOrCreate(name, config);
            self = (TreeMap<String, Object>) container.getChildObject(name, config, enclosing, getContainingName());
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

    ResultHandle generateGetOrCreate(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (container != null) {
            if (isConsumeSegment())
                body.invokeVirtualMethod(NI_PREV_METHOD, name);
            final ResultHandle enclosing = container.generateGetOrCreate(body, name, config);
            final AssignableResultHandle self = body.createVariable(TreeMap.class);
            body.assign(self, body.checkCast(
                    container.generateGetChildObject(body, name, config, enclosing, getContainingName()), Map.class));
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

    void acceptConfigurationValueIntoLeaf(final LeafConfigType leafType, final NameIterator name, final SmallRyeConfig config) {
        // leaf types directly into map values
        throw Assert.unsupported();
    }

    void generateAcceptConfigurationValueIntoLeaf(final BytecodeCreator body, final LeafConfigType leafType,
            final ResultHandle name, final ResultHandle config) {
        // leaf types directly into map values
        throw Assert.unsupported();
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle smallRyeConfig) {
        return body.newInstance(TREE_MAP_CTOR);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.set(enclosing, new TreeMap<>());
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        body.invokeStaticMethod(setter, enclosing, body.newInstance(TREE_MAP_CTOR));
    }
}
