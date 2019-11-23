package io.quarkus.deployment.configuration.definition;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.wildfly.common.Assert;

import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.util.StringUtil;

/**
 *
 */
public abstract class ClassDefinition extends Definition {
    private final Class<?> configurationClass;
    private final Map<String, ClassMember> members;

    ClassDefinition(final Builder builder) {
        super();
        final Class<?> configurationClass = builder.configurationClass;
        if (configurationClass == null) {
            throw new IllegalArgumentException("No configuration class given");
        }
        this.configurationClass = configurationClass;
        final LinkedHashMap<String, ClassMember> map = new LinkedHashMap<>(builder.members.size());
        for (Map.Entry<String, ClassMember.Specification> entry : builder.members.entrySet()) {
            map.put(entry.getKey(), entry.getValue().construct(this));
        }
        this.members = Collections.unmodifiableMap(map);
    }

    public final int getMemberCount() {
        return members.size();
    }

    public final Iterable<String> getMemberNames() {
        return members.keySet();
    }

    public final Iterable<ClassMember> getMembers() {
        return members.values();
    }

    public Class<?> getConfigurationClass() {
        return configurationClass;
    }

    public final ClassMember getMember(String name) {
        final ClassMember member = members.get(name);
        if (member == null) {
            throw new IllegalArgumentException("No member named \"" + name + "\" is present on " + configurationClass);
        }
        return member;
    }

    public static abstract class ClassMember extends Member {
        public abstract ClassDefinition getEnclosingDefinition();

        public final String getName() {
            return getField().getName();
        }

        public abstract Field getField();

        public abstract FieldDescriptor getDescriptor();

        public abstract String getPropertyName();

        public static abstract class Specification {
            Specification() {
            }

            abstract Field getField();

            abstract ClassMember construct(ClassDefinition enclosing);
        }
    }

    static abstract class LeafMember extends ClassMember {
        private final ClassDefinition classDefinition;
        private final Field field;
        private final FieldDescriptor descriptor;
        private final String propertyName;

        LeafMember(final ClassDefinition classDefinition, final Field field) {
            this.classDefinition = Assert.checkNotNullParam("classDefinition", classDefinition);
            this.field = Assert.checkNotNullParam("field", field);
            final Class<?> declaringClass = field.getDeclaringClass();
            final Class<?> configurationClass = classDefinition.configurationClass;
            if (declaringClass != configurationClass) {
                throw new IllegalArgumentException(
                        "Member declaring " + declaringClass + " does not match configuration " + configurationClass);
            }
            descriptor = FieldDescriptor.of(field);
            final ConfigItem configItem = field.getAnnotation(ConfigItem.class);
            String propertyName = ConfigItem.HYPHENATED_ELEMENT_NAME;
            if (configItem != null) {
                propertyName = configItem.name();
                if (propertyName.isEmpty()) {
                    throw reportError(field, "Invalid empty property name");
                }
            }
            if (propertyName.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                this.propertyName = StringUtil.hyphenate(field.getName());
            } else if (propertyName.equals(ConfigItem.ELEMENT_NAME)) {
                this.propertyName = field.getName();
            } else if (propertyName.equals(ConfigItem.PARENT)) {
                this.propertyName = "";
            } else {
                this.propertyName = propertyName;
            }
        }

        public Field getField() {
            return field;
        }

        public FieldDescriptor getDescriptor() {
            return descriptor;
        }

        public ClassDefinition getEnclosingDefinition() {
            return classDefinition;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public static abstract class Specification extends ClassMember.Specification {
            final Field field;

            Specification(final Field field) {
                this.field = Assert.checkNotNullParam("field", field);
            }

            Field getField() {
                return field;
            }
        }
    }

    public static final class GroupMember extends LeafMember {
        private final GroupDefinition groupDefinition;
        private final boolean optional;

        GroupMember(final ClassDefinition classDefinition, final Field field, final GroupDefinition groupDefinition,
                final boolean optional) {
            super(classDefinition, field);
            this.groupDefinition = groupDefinition;
            this.optional = optional;
        }

        public GroupDefinition getGroupDefinition() {
            return groupDefinition;
        }

        public boolean isOptional() {
            return optional;
        }

        public static final class Specification extends LeafMember.Specification {
            private final GroupDefinition groupDefinition;
            private final boolean optional;

            public Specification(final Field field, final GroupDefinition groupDefinition, final boolean optional) {
                super(field);
                this.groupDefinition = Assert.checkNotNullParam("groupDefinition", groupDefinition);
                this.optional = optional;
            }

            public boolean isOptional() {
                return optional;
            }

            ClassMember construct(final ClassDefinition enclosing) {
                return new GroupMember(enclosing, field, groupDefinition, optional);
            }
        }
    }

    public static final class ItemMember extends LeafMember {
        private final String defaultValue;

        ItemMember(final ClassDefinition classDefinition, final Field field, final String defaultValue) {
            super(classDefinition, field);
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public static final class Specification extends LeafMember.Specification {
            private final String defaultValue;

            public Specification(final Field field, final String defaultValue) {
                super(field);
                // nullable
                this.defaultValue = defaultValue;
            }

            ClassMember construct(final ClassDefinition enclosing) {
                return new ItemMember(enclosing, field, defaultValue);
            }
        }
    }

    public static final class MapMember extends ClassMember {
        private final ClassMember nested;

        MapMember(final ClassMember nested) {
            this.nested = nested;
        }

        public ClassMember getNested() {
            return nested;
        }

        public ClassDefinition getEnclosingDefinition() {
            return nested.getEnclosingDefinition();
        }

        public Field getField() {
            return nested.getField();
        }

        public FieldDescriptor getDescriptor() {
            return nested.getDescriptor();
        }

        public String getPropertyName() {
            return nested.getPropertyName();
        }

        public static final class Specification extends ClassMember.Specification {
            private final ClassMember.Specification nested;

            public Specification(final ClassMember.Specification nested) {
                this.nested = Assert.checkNotNullParam("nested", nested);
            }

            Field getField() {
                return nested.getField();
            }

            ClassMember construct(final ClassDefinition enclosing) {
                return new MapMember(nested.construct(enclosing));
            }
        }
    }

    public static abstract class Builder extends Definition.Builder {
        Builder() {
        }

        private Class<?> configurationClass;
        private final Map<String, ClassMember.Specification> members = new LinkedHashMap<>();

        public Builder setConfigurationClass(final Class<?> configurationClass) {
            this.configurationClass = configurationClass;
            return this;
        }

        public Class<?> getConfigurationClass() {
            return configurationClass;
        }

        public void addMember(ClassMember.Specification spec) {
            Assert.checkNotNullParam("spec", spec);
            members.put(spec.getField().getName(), spec);
        }

        public abstract ClassDefinition build();
    }
}
