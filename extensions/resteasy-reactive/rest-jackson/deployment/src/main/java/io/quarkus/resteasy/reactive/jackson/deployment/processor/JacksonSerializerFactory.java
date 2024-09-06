package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.IOException;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil;

/**
 * Generates an implementation of the Jackson's {@code StdSerializer} for each class that needs to be serialized in json.
 * In this way the serialization process can be performed through the ad-hoc generate serializer and then without
 * any use of reflection. For instance for a pojo like this
 *
 * <pre>{@code
 * public class Person {
 *     private String firstName;
 *
 *     &#64;JsonProperty("familyName")
 *     private String lastName;
 *
 *     private int age;
 *
 *     &#64;SecureField(rolesAllowed = "admin")
 *     private Address address;
 *
 *     public Person() {
 *     }
 *
 *     public Person(String firstName, String lastName, int age, Address address) {
 *         this.firstName = firstName;
 *         this.lastName = lastName;
 *         this.age = age;
 *         this.address = address;
 *     }
 *
 *     // getters and setters omitted
 * }
 * }</pre>
 *
 * it generates the following {@code StdSerializer} implementation
 *
 * <pre>{@code
 * public class Person$quarkusjacksonserializer extends StdSerializer {
 *     static final String[] address_ROLES_ALLOWED = new String[] { "admin" };
 *
 *     public Person$quarkusjacksonserializer() {
 *         super(Person.class);
 *     }
 *
 *     public void serialize(Object var1, JsonGenerator var2, SerializerProvider var3) throws IOException {
 *         Person var4 = (Person) var1;
 *         var2.writeStartObject();
 *         var2.writeFieldName(SerializedStrings$quarkusjacksonserializer.age);
 *         int var5 = var4.getAge();
 *         var2.writeNumber(var5);
 *         var2.writeFieldName(SerializedStrings$quarkusjacksonserializer.firstName);
 *         String var6 = var4.getFirstName();
 *         var2.writeString(var6);
 *         var2.writeFieldName(SerializedStrings$quarkusjacksonserializer.familyName);
 *         String var7 = var4.getLastName();
 *         var2.writeString(var7);
 *         if (JacksonMapperUtil.includeSecureField(address_ROLES_ALLOWED)) {
 *             var2.writeFieldName(SerializedStrings$quarkusjacksonserializer.address);
 *             Address var9 = var4.getAddress();
 *             var2.writePOJO(var9);
 *         }
 *         var2.writeEndObject();
 *     }
 * }
 *
 * public class SerializedStrings$quarkusjacksonserializer {
 *     static final SerializedString age = new SerializedString("age");
 *     static final SerializedString firstName = new SerializedString("firstName");
 *     static final SerializedString familyName = new SerializedString("familyName");
 *     static final SerializedString address = new SerializedString("address");
 * }
 * }</pre>
 *
 * Here, for performance reasons, the names of the fields to be serialized is stored as Jackson's {@code SerializedString}s
 * in an external class, and reused for each serialization, thus avoiding executing the UTF-8 encoding of the same strings
 * at each serialization.
 *
 * Note that in this case also the {@code Address} class has to be serialized in the same way, and then this factory triggers
 * the generation of a second StdSerializer also for it. More in general if during the generation of a serializer for a
 * given class it discovers a non-primitive field of another type for which a serializer hasn't been generated yet, this
 * factory enqueues a code generation also for that type. The same is valid for both arrays of that type, like
 * {@code Address[]}, and collections, like {@code List&lt;Address&gt}.
 */
public class JacksonSerializerFactory {

    private static final String SUPER_CLASS_NAME = StdSerializer.class.getName();
    private static final String JSON_GEN_CLASS_NAME = JsonGenerator.class.getName();
    private static final String SER_STRINGS_CLASS_NAME = "SerializedStrings$quarkusjacksonserializer";

    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final IndexView jandexIndex;

    private final Set<String> generatedClassNames = new HashSet<>();
    private final Map<String, Set<String>> generatedFields = new HashMap<>();
    private final Deque<ClassInfo> toBeGenerated = new ArrayDeque<>();

    public JacksonSerializerFactory(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            IndexView jandexIndex) {
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
        this.jandexIndex = jandexIndex;
    }

    public Collection<String> create(Collection<ClassInfo> classInfos) {
        Set<String> createdClasses = new HashSet<>();
        toBeGenerated.addAll(classInfos);

        while (!toBeGenerated.isEmpty()) {
            create(toBeGenerated.removeFirst()).ifPresent(createdClasses::add);
        }

        createFieldNamesClass();

        return createdClasses;
    }

    public void createFieldNamesClass() {
        if (generatedFields.isEmpty()) {
            return;
        }

        MethodDescriptor serStringCtor = MethodDescriptor.ofConstructor(SerializedString.class, String.class);

        for (Map.Entry<String, Set<String>> fieldsInPkg : generatedFields.entrySet()) {
            try (ClassCreator classCreator = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                    fieldsInPkg.getKey() + "." + SER_STRINGS_CLASS_NAME, null,
                    "java.lang.Object")) {

                MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class).setModifiers(ACC_STATIC);

                for (String field : fieldsInPkg.getValue()) {
                    FieldCreator fieldCreator = classCreator.getFieldCreator(field, SerializedString.class.getName())
                            .setModifiers(ACC_STATIC | ACC_FINAL);
                    clinit.writeStaticField(fieldCreator.getFieldDescriptor(),
                            clinit.newInstance(serStringCtor, clinit.load(field)));
                }

                clinit.returnVoid();
            }
        }
    }

    private Optional<String> create(ClassInfo classInfo) {
        String beanClassName = classInfo.name().toString();
        if (vetoedClassName(beanClassName) || !generatedClassNames.add(beanClassName)) {
            return Optional.empty();
        }

        String generatedClassName = beanClassName + "$quarkusjacksonserializer";

        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), generatedClassName, null,
                SUPER_CLASS_NAME)) {

            createConstructor(classCreator, beanClassName);
            boolean valid = createSerializeMethod(classInfo, classCreator, beanClassName);
            return valid ? Optional.of(generatedClassName) : Optional.empty();
        }
    }

    private void createConstructor(ClassCreator classCreator, String beanClassName) {
        MethodCreator constructor = classCreator.getConstructorCreator(new String[0]);
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(SUPER_CLASS_NAME, "java.lang.Class"),
                constructor.getThis(), constructor.loadClass(beanClassName));
        constructor.returnVoid();
    }

    private boolean createSerializeMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName) {
        MethodCreator serialize = classCreator.getMethodCreator("serialize", "void", "java.lang.Object", JSON_GEN_CLASS_NAME,
                "com.fasterxml.jackson.databind.SerializerProvider")
                .setModifiers(ACC_PUBLIC)
                .addException(IOException.class);
        boolean valid = serializeObject(classInfo, classCreator, beanClassName, serialize);
        serialize.returnVoid();
        return valid;
    }

    private boolean serializeObject(ClassInfo classInfo, ClassCreator classCreator, String beanClassName,
            MethodCreator serialize) {
        Set<String> serializedFields = new HashSet<>();
        ResultHandle valueHandle = serialize.checkCast(serialize.getMethodParam(0), beanClassName);
        ResultHandle jsonGenerator = serialize.getMethodParam(1);
        ResultHandle serializerProvider = serialize.getMethodParam(2);

        // jsonGenerator.writeStartObject();
        MethodDescriptor writeStartObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeStartObject", "void");
        serialize.invokeVirtualMethod(writeStartObject, jsonGenerator);

        boolean valid = serializeObjectData(classInfo, classCreator, serialize, valueHandle, jsonGenerator, serializerProvider,
                serializedFields);

        // jsonGenerator.writeEndObject();
        MethodDescriptor writeEndObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeEndObject", "void");
        serialize.invokeVirtualMethod(writeEndObject, jsonGenerator);

        if (serializedFields.isEmpty()) {
            throwExceptionForEmptyBean(beanClassName, serialize, jsonGenerator);
        }

        classCreator.getMethodCreator("<clinit>", void.class).setModifiers(ACC_STATIC).returnVoid();

        return valid;
    }

    private boolean serializeObjectData(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            ResultHandle valueHandle, ResultHandle jsonGenerator, ResultHandle serializerProvider,
            Set<String> serializedFields) {
        return serializeFields(classInfo, classCreator, serialize, valueHandle, jsonGenerator, serializerProvider,
                serializedFields) &&
                serializeMethods(classInfo, classCreator, serialize, valueHandle, jsonGenerator, serializerProvider,
                        serializedFields);
    }

    private boolean serializeFields(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            ResultHandle valueHandle,
            ResultHandle jsonGenerator, ResultHandle serializerProvider, Set<String> serializedFields) {
        for (FieldInfo fieldInfo : classFields(classInfo)) {
            if (Modifier.isStatic(fieldInfo.flags())) {
                continue;
            }
            FieldSpecs fieldSpecs = fieldSpecsFromField(classInfo, fieldInfo);
            if (fieldSpecs != null) {
                if (serializedFields.add(fieldSpecs.fieldName)) {
                    if (fieldSpecs.hasUnknownAnnotation()) {
                        return false;
                    }
                    writeField(classInfo, fieldSpecs, writeFieldBranch(classCreator, serialize, fieldSpecs), jsonGenerator,
                            serializerProvider, valueHandle);
                }
            }
        }
        return true;
    }

    private boolean serializeMethods(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            ResultHandle valueHandle,
            ResultHandle jsonGenerator, ResultHandle serializerProvider, Set<String> serializedFields) {
        for (MethodInfo methodInfo : classMethods(classInfo)) {
            if (Modifier.isStatic(methodInfo.flags())) {
                continue;
            }
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo);
            if (fieldSpecs != null && serializedFields.add(fieldSpecs.fieldName)) {
                if (fieldSpecs.hasUnknownAnnotation()) {
                    return false;
                }
                writeField(classInfo, fieldSpecs, serialize, jsonGenerator, serializerProvider, valueHandle);
            }
        }
        return true;
    }

    private void writeField(ClassInfo classInfo, FieldSpecs fieldSpecs, BytecodeCreator bytecode, ResultHandle jsonGenerator,
            ResultHandle serializerProvider, ResultHandle valueHandle) {
        String pkgName = classInfo.name().packagePrefixName().toString();
        generatedFields.computeIfAbsent(pkgName, pkg -> new HashSet<>()).add(fieldSpecs.jsonName);
        MethodDescriptor writeFieldName = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeFieldName", void.class,
                SerializableString.class);
        ResultHandle serStringHandle = bytecode.readStaticField(
                FieldDescriptor.of(pkgName + "." + SER_STRINGS_CLASS_NAME, fieldSpecs.jsonName,
                        SerializedString.class.getName()));
        bytecode.invokeVirtualMethod(writeFieldName, jsonGenerator, serStringHandle);

        ResultHandle arg = fieldSpecs.toValueReaderHandle(bytecode, valueHandle);
        String typeName = fieldSpecs.fieldType.name().toString();
        String primitiveMethodName = writeMethodForPrimitiveFields(typeName);

        if (primitiveMethodName != null) {
            MethodDescriptor primitiveWriter = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, primitiveMethodName, "void",
                    fieldSpecs.writtenType());
            bytecode.invokeVirtualMethod(primitiveWriter, jsonGenerator, arg);
            return;
        }

        registerTypeToBeGenerated(fieldSpecs.fieldType, typeName);

        MethodDescriptor writeMethod = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writePOJO",
                void.class, Object.class);
        bytecode.invokeVirtualMethod(writeMethod, jsonGenerator, arg);
    }

    private void registerTypeToBeGenerated(Type fieldType, String typeName) {
        if (!isCollectionType(fieldType, typeName)) {
            registerTypeToBeGenerated(typeName);
        }
    }

    private boolean isCollectionType(Type fieldType, String typeName) {
        if (fieldType instanceof ArrayType aType) {
            registerTypeToBeGenerated(aType.constituent());
            return true;
        }
        if (fieldType instanceof ParameterizedType pType) {
            if (pType.arguments().size() == 1 && (typeName.equals("java.util.List") ||
                    typeName.equals("java.util.Collection") || typeName.equals("java.util.Set") ||
                    typeName.equals("java.lang.Iterable"))) {
                registerTypeToBeGenerated(pType.arguments().get(0));
                return true;
            }
            if (pType.arguments().size() == 2 && typeName.equals("java.util.Map")) {
                registerTypeToBeGenerated(pType.arguments().get(1));
                registerTypeToBeGenerated(pType.arguments().get(1));
                return true;
            }
        }
        return false;
    }

    private void registerTypeToBeGenerated(Type type) {
        registerTypeToBeGenerated(type.name().toString());
    }

    private void registerTypeToBeGenerated(String typeName) {
        if (!vetoedClassName(typeName)) {
            ClassInfo classInfo = jandexIndex.getClassByName(typeName);
            if (classInfo != null && !classInfo.isEnum()) {
                toBeGenerated.add(classInfo);
            }
        }
    }

    private String writeMethodForPrimitiveFields(String typeName) {
        return switch (typeName) {
            case "java.lang.String", "char", "java.lang.Character" -> "writeString";
            case "short", "java.lang.Short", "int", "java.lang.Integer", "long", "java.lang.Long", "float",
                    "java.lang.Float", "double", "java.lang.Double" ->
                "writeNumber";
            case "boolean", "java.lang.Boolean" -> "writeBoolean";
            default -> null;
        };
    }

    private BytecodeCreator writeFieldBranch(ClassCreator classCreator, MethodCreator serialize, FieldSpecs fieldSpecs) {
        String[] rolesAllowed = fieldSpecs.rolesAllowed();
        if (rolesAllowed != null) {
            MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class).setModifiers(ACC_STATIC);

            ResultHandle rolesArray = clinit.newArray(String.class, rolesAllowed.length);
            for (int i = 0; i < rolesAllowed.length; ++i) {
                clinit.writeArrayValue(rolesArray, clinit.load(i), clinit.load(rolesAllowed[i]));
            }

            FieldCreator fieldCreator = classCreator
                    .getFieldCreator(fieldSpecs.fieldName + "_ROLES_ALLOWED", String[].class.getName())
                    .setModifiers(ACC_STATIC | ACC_FINAL);
            clinit.writeStaticField(fieldCreator.getFieldDescriptor(), rolesArray);

            ResultHandle rolesArrayReader = serialize.readStaticField(
                    FieldDescriptor.of(classCreator.getClassName(), fieldSpecs.fieldName + "_ROLES_ALLOWED",
                            String[].class.getName()));

            MethodDescriptor includeSecureField = MethodDescriptor.ofMethod(JacksonMapperUtil.class, "includeSecureField",
                    boolean.class, String[].class);
            ResultHandle included = serialize.invokeStaticMethod(includeSecureField, rolesArrayReader);
            return serialize.ifTrue(included).trueBranch();
        }
        return serialize;
    }

    private Collection<FieldInfo> classFields(ClassInfo classInfo) {
        Collection<FieldInfo> fields = new ArrayList<>();
        classFields(classInfo, fields);
        return fields;
    }

    private void classFields(ClassInfo classInfo, Collection<FieldInfo> fields) {
        fields.addAll(classInfo.fields());
        onSuperClass(classInfo, superClassInfo -> {
            classFields(superClassInfo, fields);
            return null;
        });
    }

    private Collection<MethodInfo> classMethods(ClassInfo classInfo) {
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

    private <T> T onSuperClass(ClassInfo classInfo, Function<ClassInfo, T> f) {
        Type superType = classInfo.superClassType();
        if (superType != null && !vetoedClassName(superType.name().toString())) {
            ClassInfo superClassInfo = jandexIndex.getClassByName(superType.name());
            if (superClassInfo != null) {
                return f.apply(superClassInfo);
            }
        }
        return null;
    }

    private boolean isGetterMethod(MethodInfo methodInfo) {
        String methodName = methodInfo.name();
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && methodInfo.parametersCount() == 0
                && (methodName.startsWith("get") || methodName.startsWith("is"));
    }

    private void throwExceptionForEmptyBean(String beanClassName, MethodCreator serialize, ResultHandle jsonGenerator) {
        String serializationFeatureClassName = SerializationFeature.class.getName();

        ResultHandle serializerProvider = serialize.getMethodParam(2);
        MethodDescriptor isEnabled = MethodDescriptor.ofMethod(SerializerProvider.class.getName(), "isEnabled", "boolean",
                serializationFeatureClassName);

        // if (serializerProvider.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS))
        FieldDescriptor failField = FieldDescriptor.of(serializationFeatureClassName, "FAIL_ON_EMPTY_BEANS",
                serializationFeatureClassName);
        ResultHandle failOnEmptyBeans = serialize.readStaticField(failField);
        ResultHandle isFailEnabled = serialize.invokeVirtualMethod(isEnabled, serializerProvider, failOnEmptyBeans);
        BytecodeCreator isFailEnabledBranch = serialize.ifTrue(isFailEnabled).trueBranch();

        // JavaType type = SimpleType.constructUnsafe(Class<?> cls)
        ResultHandle javaType = isFailEnabledBranch.invokeStaticMethod(
                MethodDescriptor.ofMethod(SimpleType.class, "constructUnsafe", SimpleType.class, Class.class),
                isFailEnabledBranch.loadClass(beanClassName));

        // throw InvalidDefinitionException.from(JsonGenerator g, String msg, JavaType type)
        MethodDescriptor exceptionConstructor = MethodDescriptor.ofMethod(InvalidDefinitionException.class, "from",
                InvalidDefinitionException.class, JsonGenerator.class, String.class, JavaType.class);
        String errorMsg = String.format(
                "No serializer found for class %s and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)",
                beanClassName);
        ResultHandle invalidException = isFailEnabledBranch.invokeStaticMethod(exceptionConstructor, jsonGenerator,
                isFailEnabledBranch.load(errorMsg), javaType);
        isFailEnabledBranch.throwException(invalidException);
    }

    private MethodInfo getterMethodInfo(ClassInfo classInfo, FieldInfo fieldInfo) {
        MethodInfo namedAccessor = findMethod(classInfo, fieldInfo.name());
        if (namedAccessor != null) {
            return namedAccessor;
        }
        String methodName = (isBooleanType(fieldInfo.type().name().toString()) ? "is" : "get") + ucFirst(fieldInfo.name());
        return findMethod(classInfo, methodName);
    }

    private MethodInfo findMethod(ClassInfo classInfo, String methodName, Type... parameters) {
        MethodInfo method = classInfo.method(methodName, parameters);
        return method != null ? method
                : onSuperClass(classInfo, superClassInfo -> findMethod(superClassInfo, methodName, parameters));
    }

    private static String ucFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static boolean isBooleanType(String type) {
        return type.equals("boolean") || type.equals("java.lang.Boolean");
    }

    private static boolean vetoedClassName(String className) {
        return className.startsWith("java.") || className.startsWith("jakarta.") || className.startsWith("io.vertx.core.json.");
    }

    private FieldSpecs fieldSpecsFromField(ClassInfo classInfo, FieldInfo fieldInfo) {
        MethodInfo getterMethodInfo = getterMethodInfo(classInfo, fieldInfo);
        if (getterMethodInfo != null) {
            return new FieldSpecs(fieldInfo, getterMethodInfo);
        }
        if (Modifier.isPublic(fieldInfo.flags())) {
            return new FieldSpecs(fieldInfo);
        }
        return null;
    }

    private FieldSpecs fieldSpecsFromMethod(MethodInfo methodInfo) {
        return isGetterMethod(methodInfo) ? new FieldSpecs(methodInfo) : null;
    }

    private static class FieldSpecs {

        private final String fieldName;
        private final String jsonName;
        private final Type fieldType;
        private final Map<String, AnnotationInstance> annotations = new HashMap<>();

        private MethodInfo methodInfo;
        private FieldInfo fieldInfo;

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

        private Type fieldType() {
            return fieldInfo != null ? fieldInfo.type() : methodInfo.returnType();
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
            return isBooleanType(methodInfo.returnType().toString())
                    ? methodName.substring(2, 3).toLowerCase() + methodName.substring(3)
                    : methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }

        boolean hasUnknownAnnotation() {
            return annotations.keySet().stream()
                    .anyMatch(ann -> ann.startsWith("com.fasterxml.jackson.") && !ann.equals(JsonProperty.class.getName()));
        }

        ResultHandle toValueReaderHandle(BytecodeCreator bytecode, ResultHandle valueHandle) {
            ResultHandle handle = accessorHandle(bytecode, valueHandle);

            handle = switch (fieldType.name().toString()) {
                case "char", "java.lang.Character" -> bytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Character.class, "toString", String.class, char.class), handle);
                default -> handle;
            };

            return handle;
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

        private String[] rolesAllowed() {
            AnnotationInstance secureField = annotations.get(SecureField.class.getName());
            if (secureField != null) {
                AnnotationValue rolesAllowed = secureField.value("rolesAllowed");
                return rolesAllowed != null ? rolesAllowed.asStringArray() : null;
            }
            return null;
        }
    }
}
