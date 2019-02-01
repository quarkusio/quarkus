package org.jboss.shamrock.deployment.configuration;

import static org.jboss.shamrock.deployment.steps.ConfigurationSetup.CONFIG_HELPER;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import io.smallrye.config.SmallRyeConfig;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.AccessorFinder;
import org.jboss.shamrock.runtime.configuration.NameIterator;
import org.wildfly.common.Assert;

/**
 * A configuration definition.  This class represents the configuration space as trees of nodes, where each tree
 * has a root which recursively contains all of the elements within the configuration.
 */
public class ConfigDefinition extends CompoundConfigType {
    private final TreeMap<String, Object> rootObjects = new TreeMap<>();
    private final ConfigPatternMap<LeafConfigType> leafPatterns = new ConfigPatternMap<>();
    private final IdentityHashMap<Object, ValueInfo> realizedInstances = new IdentityHashMap<>();
    private final HashMap<String, RootInfo> rootTypes = new HashMap<>();

    public ConfigDefinition() {
        super(null, null, false);
    }

    public Class<?> getItemClass() {
        throw Assert.unsupported();
    }

    public String getClassName() {
        throw Assert.unsupported();
    }

    void acceptConfigurationValueIntoLeaf(final LeafConfigType leafType, final NameIterator name, final SmallRyeConfig config) {
        // primitive/leaf values without a config group
        throw Assert.unsupported();
    }

    void generateAcceptConfigurationValueIntoLeaf(final BytecodeCreator body, final LeafConfigType leafType, final ResultHandle name, final ResultHandle config) {
        // primitive/leaf values without a config group
        throw Assert.unsupported();
    }

    Object getChildObject(final NameIterator name, final SmallRyeConfig config, final Object self, final String childName) {
        return rootObjects.get(childName);
    }

    ResultHandle generateGetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle config, final ResultHandle self, final String childName) {
        return body.readStaticField(rootTypes.get(childName).getFieldDescriptor());
    }

    TreeMap<String, Object> getOrCreate(final NameIterator name, final SmallRyeConfig config) {
        return rootObjects;
    }

    ResultHandle generateGetOrCreate(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        // always return null
        return null;
    }

    void setChildObject(final NameIterator name, final Object self, final String childName, final Object value) {
        ((TreeMap<String, Object>)self).put(childName, value);
    }

    void generateSetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle self, final String containingName, final ResultHandle value) {
        body.writeStaticField(rootTypes.get(containingName).getFieldDescriptor(), value);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        throw Assert.unsupported();
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing, final MethodDescriptor setter, final ResultHandle config) {
        throw Assert.unsupported();
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorMaker, final ResultHandle smallRyeConfig) {
        throw Assert.unsupported();
    }

    public void load() {
        loadFrom(leafPatterns);
    }

    void registerInstance(final String key, final CompoundConfigType type, final Object instance) {
        realizedInstances.put(instance, new ValueInfo(key.startsWith("shamrock.") ? key.substring("shamrock.".length()) : key, type));
    }

    public void registerRootType(final CompoundConfigType type, final AccessorFinder accessorFinder) {
        if (! type.isConsumeSegment()) throw new IllegalArgumentException("Root types cannot inherit parent name");
        final String key = type.getContainingName();
        final FieldDescriptor fieldDescriptor = FieldDescriptor.of(CONFIG_HELPER, key, Object.class);
        rootTypes.put(key, new RootInfo(type, fieldDescriptor));
    }

    public CompoundConfigType getRootType(final String key) {
        final RootInfo rootInfo = rootTypes.get(key);
        return rootInfo == null ? null : rootInfo.getRootType();
    }

    public void loadConfiguration(SmallRyeConfig config) {
        for (String propertyName : config.getPropertyNames()) {
            final NameIterator name = new NameIterator(propertyName);
            if (name.hasNext() && name.nextSegmentEquals("shamrock")) {
                name.next();
                final LeafConfigType leafType = leafPatterns.match(name);
                if (leafType != null) {
                    name.goToEnd();
                    leafType.acceptConfigurationValue(name, config);
                } else {
                    // TODO: log.warnf("Unknown configuration key \"%s\" provided", propertyName);
                }
            }
        }
        // now, ensure all roots are instantiated
        for (Map.Entry<String, RootInfo> entry : rootTypes.entrySet()) {
            entry.getValue().getRootType().getOrCreate(new NameIterator(entry.getKey(), true), config);
        }
    }

    public ConfigPatternMap<LeafConfigType> getLeafPatterns() {
        return leafPatterns;
    }

    public ConfigDefinition getConfigDefinition() {
        return this;
    }

    public CompoundConfigType getTypeOfInstance(Object configObj) {
        final ValueInfo valueInfo = realizedInstances.get(configObj);
        return valueInfo == null ? null : valueInfo.getResolvedType();
    }

    public String getAddressOfInstance(Object configObj) {
        final ValueInfo valueInfo = realizedInstances.get(configObj);
        return valueInfo == null ? null : valueInfo.getKey();
    }

    private void loadFrom(ConfigPatternMap<LeafConfigType> map) {
        final LeafConfigType matched = map.getMatched();
        if (matched != null) {
            matched.load();
        }
        for (String name : map.childNames()) {
            loadFrom(map.getChild(name));
        }
    }

    public Object getRealizedInstance(final String address) {
        final Object instance = rootObjects.get(address);
        if (instance == null) throw new IllegalStateException("Unexpected null instance at address " + address);
        return instance;
    }

    public void getRealizedInstances(BiConsumer<Object, ValueInfo> action) {
        realizedInstances.forEach(action);
    }

    static final class RootInfo {
        private final CompoundConfigType rootType;
        private final FieldDescriptor fieldDescriptor;

        public RootInfo(final CompoundConfigType rootType, final FieldDescriptor fieldDescriptor) {
            this.rootType = rootType;
            this.fieldDescriptor = fieldDescriptor;
        }

        public CompoundConfigType getRootType() {
            return rootType;
        }

        public FieldDescriptor getFieldDescriptor() {
            return fieldDescriptor;
        }
    }

    public static final class ValueInfo {
        private final String key;
        private final CompoundConfigType resolvedType;

        public ValueInfo(final String key, final CompoundConfigType resolvedType) {
            this.key = key;
            this.resolvedType = resolvedType;
        }

        public String getKey() {
            return key;
        }

        public CompoundConfigType getResolvedType() {
            return resolvedType;
        }
    }
}
