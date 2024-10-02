package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.jackson.SecureField;

public abstract class JacksonCodeGenerator {
    protected final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    protected final IndexView jandexIndex;

    protected final Set<String> generatedClassNames = new HashSet<>();
    protected final Deque<ClassInfo> toBeGenerated = new ArrayDeque<>();

    public JacksonCodeGenerator(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            IndexView jandexIndex) {
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
        this.jandexIndex = jandexIndex;
    }

    protected abstract String getSuperClassName();

    protected String[] getInterfacesNames(ClassInfo classInfo) {
        return new String[0];
    }

    protected abstract String getClassSuffix();

    public Collection<String> create(Collection<ClassInfo> classInfos) {
        Set<String> createdClasses = new HashSet<>();
        toBeGenerated.addAll(classInfos);

        while (!toBeGenerated.isEmpty()) {
            create(toBeGenerated.removeFirst()).ifPresent(createdClasses::add);
        }

        return createdClasses;
    }

    private Optional<String> create(ClassInfo classInfo) {
        String beanClassName = classInfo.name().toString();
        if (vetoedClassName(beanClassName) || !generatedClassNames.add(beanClassName)) {
            return Optional.empty();
        }

        String generatedClassName = beanClassName + getClassSuffix();

        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), generatedClassName, null,
                getSuperClassName(), getInterfacesNames(classInfo))) {

            createConstructor(classCreator, beanClassName);
            boolean valid = createSerializationMethod(classInfo, classCreator, beanClassName);
            return valid ? Optional.of(generatedClassName) : Optional.empty();
        }
    }

    private void createConstructor(ClassCreator classCreator, String beanClassName) {
        MethodCreator constructor = classCreator.getConstructorCreator(new String[0]);
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(getSuperClassName(), "java.lang.Class"),
                constructor.getThis(), constructor.loadClass(beanClassName));
        constructor.returnVoid();
    }

    protected abstract boolean createSerializationMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName);

    protected Collection<FieldInfo> classFields(ClassInfo classInfo) {
        Collection<FieldInfo> fields = new ArrayList<>();
        classFields(classInfo, fields);
        return fields;
    }

    protected void classFields(ClassInfo classInfo, Collection<FieldInfo> fields) {
        fields.addAll(classInfo.fields());
        onSuperClass(classInfo, superClassInfo -> {
            classFields(superClassInfo, fields);
            return null;
        });
    }

    protected <T> T onSuperClass(ClassInfo classInfo, Function<ClassInfo, T> f) {
        Type superType = classInfo.superClassType();
        if (superType != null && !vetoedClassName(superType.name().toString())) {
            ClassInfo superClassInfo = jandexIndex.getClassByName(superType.name());
            if (superClassInfo != null) {
                return f.apply(superClassInfo);
            }
        }
        return null;
    }

    protected Collection<MethodInfo> classMethods(ClassInfo classInfo) {
        Collection<MethodInfo> methods = new ArrayList<>();
        classMethods(classInfo, methods);
        return methods;
    }

    private void classMethods(ClassInfo classInfo, Collection<MethodInfo> methods) {
        methods.addAll(classInfo.methods());
        onSuperClass(classInfo, superClassInfo -> {
            classMethods(superClassInfo, methods);
            return null;
        });
    }

    protected MethodInfo findMethod(ClassInfo classInfo, String methodName, Type... parameters) {
        MethodInfo method = classInfo.method(methodName, parameters);
        return method != null ? method
                : onSuperClass(classInfo, superClassInfo -> findMethod(superClassInfo, methodName, parameters));
    }

    protected static String ucFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    protected static boolean vetoedClassName(String className) {
        return className.startsWith("java.") || className.startsWith("jakarta.") || className.startsWith("io.vertx.core.json.");
    }

    protected enum FieldKind {
        OBJECT(false),
        ARRAY(false),
        LIST(true),
        SET(true),
        MAP(true),
        TYPE_VARIABLE(true);

        private boolean generic;

        FieldKind(boolean generic) {
            this.generic = generic;
        }

        public boolean isGeneric() {
            return generic;
        }
    }

    protected FieldKind registerTypeToBeGenerated(Type fieldType, String typeName) {
        if (fieldType instanceof TypeVariable) {
            return FieldKind.TYPE_VARIABLE;
        }
        if (fieldType instanceof ArrayType aType) {
            registerTypeToBeGenerated(aType.constituent());
            return FieldKind.ARRAY;
        }
        if (fieldType instanceof ParameterizedType pType) {
            if (pType.arguments().size() == 1) {
                if (typeName.equals("java.util.List") || typeName.equals("java.util.Collection")
                        || typeName.equals("java.lang.Iterable")) {
                    registerTypeToBeGenerated(pType.arguments().get(0));
                    return FieldKind.LIST;
                }
                if (typeName.equals("java.util.Set")) {
                    registerTypeToBeGenerated(pType.arguments().get(0));
                    return FieldKind.SET;
                }
            }
            if (pType.arguments().size() == 2 && typeName.equals("java.util.Map")) {
                registerTypeToBeGenerated(pType.arguments().get(1));
                registerTypeToBeGenerated(pType.arguments().get(1));
                return FieldKind.MAP;
            }
        }
        registerTypeToBeGenerated(typeName);
        return FieldKind.OBJECT;
    }

    private void registerTypeToBeGenerated(Type type) {
        registerTypeToBeGenerated(type.name().toString());
    }

    private void registerTypeToBeGenerated(String typeName) {
        if (!vetoedClassName(typeName)) {
            ClassInfo classInfo = jandexIndex.getClassByName(typeName);
            if (classInfo != null && shouldGenerateCodeFor(classInfo)) {
                toBeGenerated.add(classInfo);
            }
        }
    }

    protected boolean shouldGenerateCodeFor(ClassInfo classInfo) {
        return !classInfo.isEnum();
    }

    private MethodInfo getterMethodInfo(ClassInfo classInfo, FieldInfo fieldInfo) {
        MethodInfo namedAccessor = findMethod(classInfo, fieldInfo.name());
        if (namedAccessor != null) {
            return namedAccessor;
        }
        String methodName = (fieldInfo.type().name().toString().equals("boolean") ? "is" : "get") + ucFirst(fieldInfo.name());
        return findMethod(classInfo, methodName);
    }

    protected FieldSpecs fieldSpecsFromField(ClassInfo classInfo, FieldInfo fieldInfo) {
        if (Modifier.isStatic(fieldInfo.flags())) {
            return null;
        }
        MethodInfo getterMethodInfo = getterMethodInfo(classInfo, fieldInfo);
        if (getterMethodInfo != null) {
            return new FieldSpecs(fieldInfo, getterMethodInfo);
        }
        if (Modifier.isPublic(fieldInfo.flags())) {
            return new FieldSpecs(fieldInfo);
        }
        return null;
    }

    protected static class FieldSpecs {

        final String fieldName;
        final String jsonName;
        final Type fieldType;

        private final Map<String, AnnotationInstance> annotations = new HashMap<>();

        MethodInfo methodInfo;
        FieldInfo fieldInfo;

        FieldSpecs(FieldInfo fieldInfo) {
            this(fieldInfo, null);
        }

        FieldSpecs(MethodInfo methodInfo) {
            this(null, methodInfo);
        }

        FieldSpecs(FieldInfo fieldInfo, MethodInfo methodInfo) {
            if (fieldInfo != null) {
                this.fieldInfo = fieldInfo;
                fieldInfo.annotations().forEach(a -> annotations.put(a.name().toString(), a));
            }
            if (methodInfo != null) {
                this.methodInfo = methodInfo;
                methodInfo.annotations().forEach(a -> annotations.put(a.name().toString(), a));
            }
            this.fieldType = fieldType();
            this.fieldName = fieldName();
            this.jsonName = jsonName();
        }

        public boolean isPublicField() {
            return fieldInfo != null && Modifier.isPublic(fieldInfo.flags());
        }

        private Type fieldType() {
            if (isPublicField()) {
                return fieldInfo.type();
            }
            if (methodInfo.name().startsWith("set")) {
                return methodInfo.parameterType(0);
            }
            return methodInfo.returnType();
        }

        private String jsonName() {
            AnnotationInstance jsonProperty = annotations.get(JsonProperty.class.getName());
            if (jsonProperty != null) {
                AnnotationValue value = jsonProperty.value();
                if (value != null && !value.asString().isEmpty()) {
                    return value.asString();
                }
            }
            return fieldName();
        }

        private String fieldName() {
            return fieldInfo != null ? fieldInfo.name() : fieldNameFromMethod(methodInfo);
        }

        private String fieldNameFromMethod(MethodInfo methodInfo) {
            String methodName = methodInfo.name();
            if (methodName.startsWith("is")) {
                return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
            }
            if (methodName.startsWith("get") || methodName.startsWith("set")) {
                return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            }
            return methodName;
        }

        boolean hasUnknownAnnotation() {
            return annotations.keySet().stream()
                    .anyMatch(ann -> ann.startsWith("com.fasterxml.jackson.") && !ann.equals(JsonProperty.class.getName()));
        }

        ResultHandle toValueWriterHandle(BytecodeCreator bytecode, ResultHandle valueHandle) {
            return switch (fieldType.name().toString()) {
                case "char", "java.lang.Character" -> bytecode.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(String.class, "charAt", char.class, int.class), valueHandle,
                        bytecode.load(0));
                default -> valueHandle;
            };
        }

        ResultHandle toValueReaderHandle(BytecodeCreator bytecode, ResultHandle valueHandle) {
            ResultHandle handle = accessorHandle(bytecode, valueHandle);

            return switch (fieldType.name().toString()) {
                case "char", "java.lang.Character" -> bytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Character.class, "toString", String.class, char.class), handle);
                default -> handle;
            };
        }

        private ResultHandle accessorHandle(BytecodeCreator bytecode, ResultHandle valueHandle) {
            if (methodInfo != null) {
                if (methodInfo.declaringClass().isInterface()) {
                    return bytecode.invokeInterfaceMethod(MethodDescriptor.of(methodInfo), valueHandle);
                }
                return bytecode.invokeVirtualMethod(MethodDescriptor.of(methodInfo), valueHandle);
            }
            return bytecode.readInstanceField(FieldDescriptor.of(fieldInfo), valueHandle);
        }

        String writtenType() {
            return switch (fieldType.name().toString()) {
                case "char", "java.lang.Character" -> "java.lang.String";
                case "java.lang.Integer" -> "int";
                case "java.lang.Short" -> "short";
                case "java.lang.Long" -> "long";
                case "java.lang.Double" -> "double";
                case "java.lang.Float" -> "float";
                case "java.lang.Boolean" -> "boolean";
                default -> fieldType.name().toString();
            };
        }

        String[] rolesAllowed() {
            AnnotationInstance secureField = annotations.get(SecureField.class.getName());
            if (secureField != null) {
                AnnotationValue rolesAllowed = secureField.value("rolesAllowed");
                return rolesAllowed != null ? rolesAllowed.asStringArray() : null;
            }
            return null;
        }
    }
}
