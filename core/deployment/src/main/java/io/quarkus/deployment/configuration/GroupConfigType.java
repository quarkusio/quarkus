package io.quarkus.deployment.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.wildfly.common.Assert;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 * A configuration definition node describing a configuration group.
 */
public class GroupConfigType extends CompoundConfigType {

    private final Map<String, ConfigType> fields;
    private final Class<?> class_;
    private final Constructor<?> constructor;
    private final MethodDescriptor constructorAccessor;
    private final Map<String, FieldInfo> fieldInfos;

    public GroupConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment,
            final Class<?> class_, final AccessorFinder accessorFinder) {
        super(containingName, container, consumeSegment);
        Assert.checkNotNullParam("containingName", containingName);
        Assert.checkNotNullParam("container", container);
        Assert.checkNotNullParam("class_", class_);
        Assert.checkNotNullParam("accessorFinder", accessorFinder);
        fields = new HashMap<>();
        this.class_ = class_;
        try {
            constructor = class_.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Constructor of " + class_ + " is missing");
        }
        if ((constructor.getModifiers() & Modifier.PRIVATE) != 0) {
            throw new IllegalArgumentException("Constructor of " + class_ + " must not be private");
        } else if ((constructor.getModifiers() & Modifier.PUBLIC) == 0) {
            constructor.setAccessible(true);
        }
        constructorAccessor = accessorFinder.getConstructorFor(MethodDescriptor.ofConstructor(class_));
        fieldInfos = new HashMap<>();
        for (Field field : class_.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if ((modifiers & Modifier.STATIC) == 0) {
                // consider this one
                if ((modifiers & Modifier.PRIVATE) != 0) {
                    throw new IllegalArgumentException(
                            "Field \"" + field.getName() + "\" of " + class_ + " must not be private");
                }
                field.setAccessible(true);
                final FieldDescriptor descr = FieldDescriptor.of(field);
                fieldInfos.put(field.getName(),
                        new FieldInfo(field, accessorFinder.getSetterFor(descr), accessorFinder.getGetterFor(descr)));
            }
        }
    }

    public void load() throws ClassNotFoundException {
        assert class_ != null && constructor != null;
        if (!fieldInfos.keySet().containsAll(fields.keySet())) {
            final TreeSet<String> missing = new TreeSet<>(fields.keySet());
            missing.removeAll(fieldInfos.keySet());
            throw new IllegalArgumentException("Fields missing from " + class_ + ": " + missing);
        }
        if (!fields.keySet().containsAll(fieldInfos.keySet())) {
            final TreeSet<String> extra = new TreeSet<>(fieldInfos.keySet());
            extra.removeAll(fields.keySet());
            throw new IllegalArgumentException("Extra unknown fields on " + class_ + ": " + extra);
        }
        for (ConfigType node : fields.values()) {
            node.load();
        }
    }

    public ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle smallRyeConfig) {
        final ResultHandle instance = body
                .invokeStaticMethod(accessorFinder.getConstructorFor(MethodDescriptor.ofConstructor(class_)));
        for (Map.Entry<String, ConfigType> entry : fields.entrySet()) {
            final String fieldName = entry.getKey();
            final ConfigType fieldType = entry.getValue();
            final FieldDescriptor fieldDescriptor = FieldDescriptor.of(fieldInfos.get(fieldName).getField());
            final ResultHandle value = fieldType.writeInitialization(body, accessorFinder, smallRyeConfig);
            body.invokeStaticMethod(accessorFinder.getSetterFor(fieldDescriptor), instance, value);
        }
        return instance;
    }

    public ConfigType getField(String name) {
        return fields.get(name);
    }

    public void addField(ConfigType node) {
        final String containingName = node.getContainingName();
        final ConfigType existing = fields.putIfAbsent(containingName, node);
        if (existing != null) {
            throw new IllegalArgumentException("Cannot add duplicate field \"" + containingName + "\" to " + this);
        }
    }

    private Field findField(final String name) {
        if (class_ == null)
            throw notLoadedException();
        final FieldInfo fieldInfo = fieldInfos.get(name);
        if (fieldInfo == null)
            throw new IllegalStateException("Missing field " + name + " on " + class_);
        return fieldInfo.getField();
    }

    private Object create(final SmallRyeConfig config) {
        Object self;
        try {
            self = constructor.newInstance();
        } catch (InstantiationException e) {
            throw toError(e);
        } catch (IllegalAccessException e) {
            throw toError(e);
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        for (Map.Entry<String, ConfigType> entry : fields.entrySet()) {
            entry.getValue().getDefaultValueIntoEnclosingGroup(self, config, findField(entry.getKey()));
        }
        return self;
    }

    private ResultHandle generateCreate(final BytecodeCreator body, final ResultHandle config) {
        final ResultHandle self = body.invokeStaticMethod(constructorAccessor);
        for (Map.Entry<String, ConfigType> entry : fields.entrySet()) {
            final ConfigType childType = entry.getValue();
            final MethodDescriptor setter = fieldInfos.get(entry.getKey()).getSetter();
            childType.generateGetDefaultValueIntoEnclosingGroup(body, self, setter, config);
        }
        return self;
    }

    Object getChildObject(final NameIterator name, final SmallRyeConfig config, final Object self, final String childName) {
        final Field field = findField(childName);
        Object val = getFromField(field, self);
        if (val == null) {
            final ConfigType childType = getField(childName);
            childType.getDefaultValueIntoEnclosingGroup(self, config, field);
            val = getFromField(field, self);
        }
        return val;
    }

    ResultHandle generateGetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle config,
            final ResultHandle self, final String childName) {
        final AssignableResultHandle val = body.createVariable(Object.class);
        final FieldInfo fieldInfo = fieldInfos.get(childName);
        body.assign(val, body.invokeStaticMethod(fieldInfo.getGetter(), self));
        try (BytecodeCreator isNull = body.ifNull(val).trueBranch()) {
            final ConfigType childType = getField(childName);
            childType.generateGetDefaultValueIntoEnclosingGroup(isNull, self, fieldInfo.getSetter(), config);
            isNull.assign(val, isNull.invokeStaticMethod(fieldInfo.getGetter(), self));
        }
        return val;
    }

    private static Object getFromField(Field field, Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    Object getOrCreate(final NameIterator name, final SmallRyeConfig config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            name.previous();
        final Object enclosing = container.getOrCreate(name, config);
        Object self = container.getChildObject(name, config, enclosing, getContainingName());
        if (isConsumeSegment())
            name.next();
        if (self == null) {
            // it's a map, and it doesn't contain our key.
            self = create(config);
            if (isConsumeSegment())
                name.previous();
            container.setChildObject(name, enclosing, getContainingName(), self);
            if (isConsumeSegment())
                name.next();
        }
        return self;
    }

    ResultHandle generateGetOrCreate(final BytecodeCreator body, final ResultHandle name, final ResultHandle config) {
        final CompoundConfigType container = getContainer();
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_PREV_METHOD, name);
        final ResultHandle enclosing = container.generateGetOrCreate(body, name, config);
        final AssignableResultHandle var = body.createVariable(Object.class);
        body.assign(var, container.generateGetChildObject(body, name, config, enclosing, getContainingName()));
        if (isConsumeSegment())
            body.invokeVirtualMethod(NI_NEXT_METHOD, name);
        if (container.getClass() == MapConfigType.class) {
            // it could be null
            try (BytecodeCreator createBranch = body.ifNull(var).trueBranch()) {
                createBranch.assign(var, generateCreate(createBranch, config));
                if (isConsumeSegment())
                    createBranch.invokeVirtualMethod(NI_PREV_METHOD, name);
                container.generateSetChildObject(createBranch, name, enclosing, getContainingName(), var);
                if (isConsumeSegment())
                    createBranch.invokeVirtualMethod(NI_NEXT_METHOD, name);
            }
        }
        return var;
    }

    void acceptConfigurationValueIntoLeaf(final LeafConfigType leafType, final NameIterator name, final SmallRyeConfig config) {
        final FieldInfo fieldInfo = fieldInfos.get(leafType.getContainingName());
        leafType.acceptConfigurationValueIntoGroup(getOrCreate(name, config), fieldInfo.getField(), name, config);
    }

    void generateAcceptConfigurationValueIntoLeaf(final BytecodeCreator body, final LeafConfigType leafType,
            final ResultHandle name, final ResultHandle config) {
        final FieldInfo fieldInfo = fieldInfos.get(leafType.getContainingName());
        leafType.generateAcceptConfigurationValueIntoGroup(body, generateGetOrCreate(body, name, config), fieldInfo.getSetter(),
                name, config);
    }

    void setChildObject(final NameIterator name, final Object self, final String containingName, final Object value) {
        try {
            findField(containingName).set(self, value);
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateSetChildObject(final BytecodeCreator body, final ResultHandle name, final ResultHandle self,
            final String containingName, final ResultHandle value) {
        body.invokeStaticMethod(fieldInfos.get(containingName).getSetter(), self, value);
    }

    void getDefaultValueIntoEnclosingGroup(final Object enclosing, final SmallRyeConfig config, final Field field) {
        try {
            field.set(enclosing, create(config));
        } catch (IllegalAccessException e) {
            throw toError(e);
        }
    }

    void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle config) {
        final ResultHandle self = generateCreate(body, config);
        body.invokeStaticMethod(setter, enclosing, self);
    }

    static final class FieldInfo {
        private final Field field;
        private final MethodDescriptor setter;
        private final MethodDescriptor getter;

        public FieldInfo(final Field field, final MethodDescriptor setter, final MethodDescriptor getter) {
            this.field = field;
            this.setter = setter;
            this.getter = getter;
        }

        public Field getField() {
            return field;
        }

        public MethodDescriptor getSetter() {
            return setter;
        }

        public MethodDescriptor getGetter() {
            return getter;
        }
    }
}
