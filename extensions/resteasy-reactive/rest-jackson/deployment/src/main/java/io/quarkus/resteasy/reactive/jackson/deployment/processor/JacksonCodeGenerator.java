package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isApplicationClass;

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
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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

    private static final Logger log = Logger.getLogger(JacksonCodeGenerator.class);

    private static final Set<String> SUPPORTED_JACKSON_ANNOTATIONS = Set.of(
            JacksonAnnotation.class.getName(),
            JacksonAnnotationsInside.class.getName(),
            JsonAlias.class.getName(),
            JsonAnyGetter.class.getName(),
            JsonAnySetter.class.getName(),
            JsonBackReference.class.getName(),
            JsonClassDescription.class.getName(),
            JsonCreator.class.getName(),
            JsonFormat.class.getName(),
            JsonGetter.class.getName(),
            JsonIgnore.class.getName(),
            JsonIgnoreProperties.class.getName(),
            JsonIgnoreType.class.getName(),
            JsonInclude.class.getName(),
            JsonManagedReference.class.getName(),
            JsonNaming.class.getName(),
            JsonProperty.class.getName(),
            JsonPropertyDescription.class.getName(),
            JsonPropertyOrder.class.getName(),
            JsonRawValue.class.getName(),
            JsonSetter.class.getName(),
            JsonSubTypes.class.getName(),
            JsonTypeInfo.class.getName(),
            JsonTypeName.class.getName(),
            JsonUnwrapped.class.getName(),
            JsonValue.class.getName(),
            JsonView.class.getName());

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
        if (vetoedClass(classInfo, beanClassName) || !generatedClassNames.add(beanClassName)) {
            return Optional.empty();
        }
        Optional<String> unknownAnnotation = findUnknownAnnotation(classInfo);
        if (unknownAnnotation.isPresent()) {
            log.infof("Skipping generation of reflection-free Jackson serializer for class %s" +
                    " because it contains the unsupported Jackson annotation %s", beanClassName, unknownAnnotation.get());
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

    protected static boolean vetoedClass(ClassInfo classInfo, String className) {
        return classInfo.isAbstract() || classInfo.isInterface() || vetoedClassName(className);
    }

    private static boolean vetoedClassName(String className) {
        return className.startsWith("java.") || className.startsWith("jakarta.") || className.startsWith("io.vertx.core.json.");
    }

    private static Optional<String> findUnknownAnnotation(ClassInfo classInfo) {
        return classInfo.annotations().stream()
                .map(a -> a.name().toString())
                .filter(FieldSpecs::isUnknownAnnotation)
                .findFirst();
    }

    protected enum FieldKind {
        OBJECT(false),
        ARRAY(false),
        LIST(true),
        SET(true),
        MAP(true),
        OPTIONAL(true),
        TYPE_VARIABLE(true);

        private final boolean generic;

        FieldKind(boolean generic) {
            this.generic = generic;
        }

        public boolean isGeneric() {
            return generic;
        }
    }

    private static final DotName COLLECTION_NAME = DotName.createSimple(Collection.class);
    private static final DotName SET_NAME = DotName.createSimple(Set.class);
    private static final DotName MAP_NAME = DotName.createSimple(Map.class);

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
                if (isAssignableTo(typeName, SET_NAME)) {
                    registerTypeToBeGenerated(pType.arguments().get(0));
                    return FieldKind.SET;
                }
                if (typeName.equals("java.lang.Iterable") || isAssignableTo(typeName, COLLECTION_NAME)) {
                    registerTypeToBeGenerated(pType.arguments().get(0));
                    return FieldKind.LIST;
                }
                if (Optional.class.getName().equals(typeName)) {
                    registerTypeToBeGenerated(pType.arguments().get(0));
                    return FieldKind.OPTIONAL;
                }
            }
            if (pType.arguments().size() == 2 && isAssignableTo(typeName, MAP_NAME)) {
                registerTypeToBeGenerated(pType.arguments().get(0));
                registerTypeToBeGenerated(pType.arguments().get(1));
                return FieldKind.MAP;
            }
        }
        registerTypeToBeGenerated(typeName);
        return FieldKind.OBJECT;
    }

    private boolean isAssignableTo(String typeName, DotName targetName) {
        if (typeName.equals(targetName.toString())) {
            return true;
        }
        ClassInfo classInfo = jandexIndex.getClassByName(typeName);
        if (classInfo == null) {
            return false;
        }
        // check superclass
        if (classInfo.superName() != null && isAssignableTo(classInfo.superName().toString(), targetName)) {
            return true;
        }
        // check interfaces
        for (DotName iface : classInfo.interfaceNames()) {
            if (isAssignableTo(iface.toString(), targetName)) {
                return true;
            }
        }
        return false;
    }

    private void registerTypeToBeGenerated(Type type) {
        registerTypeToBeGenerated(type.name().toString());
    }

    private void registerTypeToBeGenerated(String typeName) {
        ClassInfo classInfo = jandexIndex.getClassByName(typeName);
        if (classInfo == null || !isRuntimeAccessible(classInfo, typeName)) {
            return;
        }
        if (vetoedClass(classInfo, typeName)) {
            if (classInfo.isSealed()) {
                for (DotName subClassName : classInfo.permittedSubclasses()) {
                    registerTypeToBeGenerated(subClassName.toString());
                }
            }
            return;
        }
        if (shouldGenerateCodeFor(classInfo)) {
            toBeGenerated.add(classInfo);
        }
    }

    private static boolean isRuntimeAccessible(ClassInfo classInfo, String className) {
        return Modifier.isPublic(classInfo.flags()) || isApplicationClass(className);
    }

    protected boolean shouldGenerateCodeFor(ClassInfo classInfo) {
        return !classInfo.isEnum();
    }

    protected static String anyGetterBackingFieldName(MethodInfo anyGetterMethod) {
        String methodName = anyGetterMethod.name();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }
        return methodName;
    }

    private MethodInfo getterMethodInfo(ClassInfo classInfo, FieldInfo fieldInfo) {
        MethodInfo namedAccessor = findMethod(classInfo, fieldInfo.name());
        if (namedAccessor != null
                && (classInfo.isRecord() || namedAccessor.hasAnnotation(JsonProperty.class)
                        || fieldInfo.hasAnnotation(JsonProperty.class))) {
            return namedAccessor;
        }
        String methodName = (fieldInfo.type().name().toString().equals("boolean") ? "is" : "get") + ucFirst(fieldInfo.name());
        return findMethod(classInfo, methodName);
    }

    protected Optional<MethodInfo> findConstructor(ClassInfo classInfo) {
        Optional<MethodInfo> ctorOpt = classInfo.constructors().stream()
                .filter(ctor -> Modifier.isPublic(ctor.flags()) && ctor.hasAnnotation(JsonCreator.class))
                .findFirst();

        if (ctorOpt.isEmpty()) {
            if (classInfo.hasNoArgsConstructor() && !classInfo.isRecord()) {
                return classInfo.constructors().stream()
                        .filter(ctor -> ctor.parametersCount() == 0)
                        .findFirst();
            }
            ctorOpt = classInfo.isRecord() ? Optional.of(classInfo.canonicalRecordConstructor())
                    : classInfo.constructors().stream().filter(ctor -> Modifier.isPublic(ctor.flags())).findFirst();
        }
        return ctorOpt;
    }

    protected PropertyNamingStrategy getNamingStrategy(ClassInfo classInfo) {
        AnnotationInstance jsonNaming = classInfo.annotation(JsonNaming.class);
        if (jsonNaming == null || jsonNaming.value() == null) {
            return null;
        }
        String strategyClassName = jsonNaming.value().asClass().name().toString();
        try {
            return (PropertyNamingStrategy) Class.forName(strategyClassName)
                    .getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    protected static String[] getPropertyOrder(ClassInfo classInfo) {
        AnnotationInstance ann = classInfo.declaredAnnotation(JsonPropertyOrder.class);
        if (ann == null || ann.value() == null) {
            return null;
        }
        return ann.value().asStringArray();
    }

    protected boolean isFieldTypeIgnored(FieldSpecs fieldSpecs) {
        ClassInfo typeInfo = jandexIndex.getClassByName(fieldSpecs.fieldType.name());
        return typeInfo != null && typeInfo.hasAnnotation(JsonIgnoreType.class);
    }

    protected static String getClassIncludeValue(ClassInfo classInfo) {
        AnnotationInstance include = classInfo.declaredAnnotation(JsonInclude.class);
        if (include == null || include.value() == null) {
            return null;
        }
        String includeValue = include.value().asEnum();
        return switch (includeValue) {
            case "NON_NULL" -> "NON_NULL";
            case "NON_EMPTY" -> "NON_EMPTY";
            case "NON_ABSENT" -> "NON_ABSENT";
            default -> null;
        };
    }

    protected boolean isEnumType(String typeName) {
        ClassInfo ci = jandexIndex.getClassByName(typeName);
        return ci != null && ci.isEnum();
    }

    protected MethodInfo findAnyGetterMethod(ClassInfo classInfo) {
        for (MethodInfo method : classMethods(classInfo)) {
            if (method.hasAnnotation(JsonAnyGetter.class)
                    && method.parametersCount() == 0
                    && !java.lang.reflect.Modifier.isStatic(method.flags())) {
                return method;
            }
        }
        return null;
    }

    protected static Set<String> getIgnoredProperties(ClassInfo classInfo) {
        AnnotationInstance ann = classInfo.declaredAnnotation(JsonIgnoreProperties.class);
        if (ann == null || ann.value() == null) {
            return Set.of();
        }
        String[] names = ann.value().asStringArray();
        return names.length == 0 ? Set.of() : Set.of(names);
    }

    protected static boolean shouldIgnoreUnknownProperties(ClassInfo classInfo) {
        AnnotationInstance ann = classInfo.declaredAnnotation(JsonIgnoreProperties.class);
        return ann != null && ann.value("ignoreUnknown") != null && ann.value("ignoreUnknown").asBoolean();
    }

    protected FieldSpecs fieldSpecsFromField(ClassInfo classInfo, MethodInfo constructor, FieldInfo fieldInfo,
            PropertyNamingStrategy namingStrategy) {
        if (Modifier.isStatic(fieldInfo.flags())) {
            return null;
        }
        MethodInfo getterMethodInfo = getterMethodInfo(classInfo, fieldInfo);
        if (getterMethodInfo != null) {
            return new FieldSpecs(constructor, fieldInfo, getterMethodInfo, namingStrategy);
        }
        if (Modifier.isPublic(fieldInfo.flags())) {
            return new FieldSpecs(fieldInfo, namingStrategy);
        }
        return null;
    }

    protected FieldSpecs fieldSpecsFromFieldParam(MethodParameterInfo paramInfo, PropertyNamingStrategy namingStrategy) {
        return new FieldSpecs(paramInfo, namingStrategy);
    }

    protected static class FieldSpecs {

        final String fieldName;
        final String jsonName;
        final boolean hasExplicitJsonName;
        final String[] aliases;
        final Type fieldType;

        private final Map<String, AnnotationInstance> annotations = new HashMap<>();

        MethodInfo methodInfo;
        FieldInfo fieldInfo;

        FieldSpecs(FieldInfo fieldInfo) {
            this(null, fieldInfo, null, null);
        }

        FieldSpecs(FieldInfo fieldInfo, PropertyNamingStrategy namingStrategy) {
            this(null, fieldInfo, null, namingStrategy);
        }

        FieldSpecs(MethodInfo methodInfo) {
            this(null, null, methodInfo, null);
        }

        FieldSpecs(MethodInfo constructor, FieldInfo fieldInfo, MethodInfo methodInfo, PropertyNamingStrategy namingStrategy) {
            if (fieldInfo != null) {
                this.fieldInfo = fieldInfo;
                readAnnotations(fieldInfo);
            }
            if (methodInfo != null) {
                this.methodInfo = methodInfo;
                readAnnotations(methodInfo);
            }
            this.fieldType = fieldType();
            this.fieldName = fieldName();
            JsonNameResult result = jsonName(constructor, namingStrategy);
            this.jsonName = result.name;
            this.hasExplicitJsonName = result.explicit;
            this.aliases = jsonAliases();
        }

        FieldSpecs(MethodParameterInfo paramInfo, PropertyNamingStrategy namingStrategy) {
            readAnnotations(paramInfo);
            this.fieldType = paramInfo.type();
            this.fieldName = paramInfo.name();
            JsonNameResult result = jsonName(null, namingStrategy);
            this.jsonName = result.name;
            this.hasExplicitJsonName = result.explicit;
            this.aliases = jsonAliases();
        }

        private void readAnnotations(AnnotationTarget target) {
            target.annotations().forEach(a -> annotations.put(a.name().toString(), a));
        }

        public boolean isPublicField() {
            return fieldInfo != null && Modifier.isPublic(fieldInfo.flags());
        }

        private Type fieldType() {
            if (isPublicField()) {
                return fieldInfo.type();
            }
            if (methodInfo.parametersCount() == 1 && methodInfo.name().startsWith("set")) {
                return methodInfo.parameterType(0);
            }
            return methodInfo.returnType();
        }

        private String[] jsonAliases() {
            AnnotationInstance jsonAlias = annotations.get(JsonAlias.class.getName());
            if (jsonAlias != null) {
                AnnotationValue value = jsonAlias.value();
                if (value != null) {
                    return value.asStringArray();
                }
            }
            return new String[0];
        }

        private record JsonNameResult(String name, boolean explicit) {
        }

        private JsonNameResult jsonName(MethodInfo constructor, PropertyNamingStrategy namingStrategy) {
            AnnotationInstance jsonProperty = annotations.get(JsonProperty.class.getName());
            if (jsonProperty == null) {
                jsonProperty = annotations.get(JsonGetter.class.getName());
            }
            if (jsonProperty == null) {
                jsonProperty = annotations.get(JsonSetter.class.getName());
            }
            if (jsonProperty == null && constructor != null) {
                jsonProperty = constructor.parameters().stream()
                        .filter(parameter -> parameter.name().equals(fieldName)).findFirst()
                        .map(parameter -> parameter.annotation(JsonProperty.class.getName()))
                        .orElse(null);
            }

            if (jsonProperty != null) {
                AnnotationValue value = jsonProperty.value();
                if (value != null && !value.asString().isEmpty()) {
                    return new JsonNameResult(value.asString(), true);
                }
            }
            if (namingStrategy != null) {
                return new JsonNameResult(namingStrategy.nameForField(null, null, fieldName), true);
            }
            return new JsonNameResult(fieldName, false);
        }

        private String fieldName() {
            return fieldInfo != null ? fieldInfo.name() : fieldNameFromMethod(methodInfo);
        }

        private String fieldNameFromMethod(MethodInfo methodInfo) {
            String methodName = methodInfo.name();
            if (methodName.equals("get") || methodName.equals("set") || methodName.equals("is")) {
                return methodName;
            }
            if (methodName.startsWith("is")) {
                return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
            }
            if (methodName.startsWith("get") || methodName.startsWith("set")) {
                return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            }
            return methodName;
        }

        boolean isIgnoredField() {
            return annotations.get(JsonIgnore.class.getName()) != null;
        }

        boolean isUnwrapped() {
            return annotations.get(JsonUnwrapped.class.getName()) != null;
        }

        boolean isBackReference() {
            return annotations.get(JsonBackReference.class.getName()) != null;
        }

        boolean isRawValue() {
            return annotations.get(JsonRawValue.class.getName()) != null;
        }

        boolean isFormatShapeNumber() {
            AnnotationInstance format = annotations.get(JsonFormat.class.getName());
            if (format == null) {
                return false;
            }
            AnnotationValue shape = format.value("shape");
            return shape != null && "NUMBER".equals(shape.asEnum());
        }

        String jsonIncludeValue() {
            AnnotationInstance include = annotations.get(JsonInclude.class.getName());
            if (include == null || include.value() == null) {
                return null;
            }
            String includeValue = include.value().asEnum();
            return switch (includeValue) {
                case "NON_NULL" -> "NON_NULL";
                case "NON_EMPTY" -> "NON_EMPTY";
                case "NON_ABSENT" -> "NON_ABSENT";
                default -> null;
            };
        }

        static boolean isUnknownAnnotation(String ann) {
            if (ann.startsWith("com.fasterxml.jackson.")) {
                return !SUPPORTED_JACKSON_ANNOTATIONS.contains(ann);
            }
            return ann.startsWith("jakarta.persistence.");
        }

        String[] viewClasses() {
            AnnotationInstance jsonView = annotations.get(JsonView.class.getName());
            if (jsonView == null || jsonView.value() == null) {
                return null;
            }
            Type[] types = jsonView.value().asClassArray();
            String[] classNames = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                classNames[i] = types[i].name().toString();
            }
            return classNames;
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
