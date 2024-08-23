package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.core.JsonGenerator;
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
 *     private String lastName;
 *     private int age;
 *
 *     @SecureField(rolesAllowed = "admin")
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
 *     public Person$quarkusjacksonserializer() {
 *         super(Person.class);
 *     }
 *
 *     public void serialize(Object var1, JsonGenerator var2, SerializerProvider var3) throws IOException {
 *         Person var4 = (Person) var1;
 *         var2.writeStartObject();
 *         int var5 = var4.getAge();
 *         var2.writeNumberField("age", var5);
 *         String var6 = var4.getFirstName();
 *         var2.writeStringField("firstName", var6);
 *         String var7 = var4.getLastName();
 *         var2.writeStringField("lastName", var7);
 *         String[] var8 = new String[] { "admin" };
 *         Address var9 = var4.getAddress();
 *         if (JacksonMapperUtil.includeSecureField(var8)) {
 *             var2.writePOJOField("address", var9);
 *         }
 *         var2.writeEndObject();
 *     }
 * }
 * }</pre>
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

    private final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    private final IndexView jandexIndex;

    private final Set<String> generatedClassNames = new HashSet<>();
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

        return createdClasses;
    }

    public Optional<String> create(ClassInfo classInfo) {
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
                "com.fasterxml.jackson.databind.SerializerProvider");
        serialize.setModifiers(ACC_PUBLIC);
        serialize.addException(IOException.class);
        boolean valid = serializeObject(classInfo, beanClassName, serialize);
        serialize.returnVoid();
        return valid;
    }

    private boolean serializeObject(ClassInfo classInfo, String beanClassName, MethodCreator serialize) {
        Set<String> serializedFields = new HashSet<>();
        ResultHandle valueHandle = serialize.checkCast(serialize.getMethodParam(0), beanClassName);
        ResultHandle jsonGenerator = serialize.getMethodParam(1);
        ResultHandle serializerProvider = serialize.getMethodParam(2);

        // jsonGenerator.writeStartObject();
        MethodDescriptor writeStartObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeStartObject", "void");
        serialize.invokeVirtualMethod(writeStartObject, jsonGenerator);

        boolean valid = serializeObjectData(classInfo, serialize, valueHandle, jsonGenerator, serializerProvider,
                serializedFields);

        // jsonGenerator.writeEndObject();
        MethodDescriptor writeEndObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeEndObject", "void");
        serialize.invokeVirtualMethod(writeEndObject, jsonGenerator);

        if (serializedFields.isEmpty()) {
            throwExceptionForEmptyBean(beanClassName, serialize, jsonGenerator);
        }

        return valid;
    }

    private boolean serializeObjectData(ClassInfo classInfo, MethodCreator serialize,
            ResultHandle valueHandle, ResultHandle jsonGenerator, ResultHandle serializerProvider,
            Set<String> serializedFields) {
        return serializeFields(classInfo, serialize, valueHandle, jsonGenerator, serializerProvider, serializedFields) &&
                serializeMethods(classInfo, serialize, valueHandle, jsonGenerator, serializerProvider, serializedFields);
    }

    private boolean serializeFields(ClassInfo classInfo, MethodCreator serialize, ResultHandle valueHandle,
            ResultHandle jsonGenerator, ResultHandle serializerProvider, Set<String> serializedFields) {
        for (FieldInfo fieldInfo : classFields(classInfo)) {
            if (Modifier.isStatic(fieldInfo.flags())) {
                continue;
            }
            AnnotationTarget target = valueReader(classInfo, fieldInfo);
            if (target != null) {
                String fieldName = fieldInfo.name();
                if (serializedFields.add(fieldName)) {
                    if (hasUnknownAnnotation(fieldInfo) || (fieldInfo != target && hasUnknownAnnotation(target))) {
                        return false;
                    }
                    ResultHandle arg = toValueReaderHandle(target, serialize, valueHandle);
                    writeField(fieldInfo.type(), fieldName, writeFieldBranch(serialize, fieldInfo, target), jsonGenerator,
                            serializerProvider, arg);
                }
            }
        }
        return true;
    }

    private boolean serializeMethods(ClassInfo classInfo, MethodCreator serialize, ResultHandle valueHandle,
            ResultHandle jsonGenerator, ResultHandle serializerProvider, Set<String> serializedFields) {
        for (MethodInfo methodInfo : classMethods(classInfo)) {
            if (Modifier.isStatic(methodInfo.flags())) {
                continue;
            }
            String fieldName = fieldNameFromMethod(methodInfo);
            if (fieldName != null && serializedFields.add(fieldName)) {
                if (hasUnknownAnnotation(methodInfo)) {
                    return false;
                }
                ResultHandle arg = serialize.invokeVirtualMethod(MethodDescriptor.of(methodInfo), valueHandle);
                writeField(methodInfo.returnType(), fieldName, serialize, jsonGenerator, serializerProvider, arg);
            }
        }
        return true;
    }

    private void writeField(Type fieldType, String fieldName, BytecodeCreator bytecode, ResultHandle jsonGenerator,
            ResultHandle serializerProvider, ResultHandle fieldReader) {
        String typeName = fieldType.name().toString();
        String primitiveMethodName = writeMethodForPrimitiveFields(typeName);
        if (primitiveMethodName != null) {
            MethodDescriptor primitiveWriter = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, primitiveMethodName, "void",
                    "java.lang.String", typeName);
            bytecode.invokeVirtualMethod(primitiveWriter, jsonGenerator, bytecode.load(fieldName), fieldReader);
            return;
        }

        registerTypeToBeGenerated(fieldType, typeName);

        MethodDescriptor writeMethod = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writePOJOField",
                void.class, String.class, Object.class);
        bytecode.invokeVirtualMethod(writeMethod, jsonGenerator, bytecode.load(fieldName), fieldReader);
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
            case "java.lang.String" -> "writeStringField";
            case "short", "java.lang.Short", "int", "java.lang.Integer", "long", "java.lang.Long", "float",
                    "java.lang.Float", "double", "java.lang.Double" ->
                "writeNumberField";
            case "boolean", "java.lang.Boolean" -> "writeBooleanField";
            default -> null;
        };
    }

    private boolean hasUnknownAnnotation(AnnotationTarget target) {
        return target.annotations().stream().anyMatch(ann -> ann.name().toString().startsWith("com.fasterxml.jackson."));
    }

    private BytecodeCreator writeFieldBranch(MethodCreator serialize, FieldInfo fieldInfo, AnnotationTarget target) {
        String[] rolesAllowed = rolesAllowed(fieldInfo, target);
        if (rolesAllowed != null) {
            ResultHandle rolesArray = serialize.newArray(String.class, rolesAllowed.length);
            for (int i = 0; i < rolesAllowed.length; ++i) {
                serialize.writeArrayValue(rolesArray, serialize.load(i), serialize.load(rolesAllowed[i]));
            }

            MethodDescriptor includeSecureField = MethodDescriptor.ofMethod(JacksonMapperUtil.class, "includeSecureField",
                    boolean.class, String[].class);
            ResultHandle included = serialize.invokeStaticMethod(includeSecureField, rolesArray);
            return serialize.ifTrue(included).trueBranch();
        }
        return serialize;
    }

    private String[] rolesAllowed(FieldInfo fieldInfo, AnnotationTarget target) {
        AnnotationInstance secureField = fieldInfo.annotation(SecureField.class);
        if (secureField == null && target != fieldInfo) {
            secureField = target.annotation(SecureField.class);
        }
        if (secureField != null) {
            AnnotationValue rolesAllowed = secureField.value("rolesAllowed");
            return rolesAllowed != null ? rolesAllowed.asStringArray() : null;
        }
        return null;
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

    private String fieldNameFromMethod(MethodInfo methodInfo) {
        if (isGetterMethod(methodInfo)) {
            String methodName = methodInfo.name();
            return isBooleanType(methodInfo.returnType().toString())
                    ? methodName.substring(2, 3).toLowerCase() + methodName.substring(3)
                    : methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }
        return null;
    }

    private AnnotationTarget valueReader(ClassInfo classInfo, FieldInfo fieldInfo) {
        MethodInfo getterMethodInfo = getterMethodInfo(classInfo, fieldInfo);
        if (getterMethodInfo != null) {
            return getterMethodInfo;
        }
        if (Modifier.isPublic(fieldInfo.flags())) {
            return fieldInfo;
        }
        return null;
    }

    private ResultHandle toValueReaderHandle(Object member, BytecodeCreator serialize, ResultHandle valueHandle) {
        if (member instanceof MethodInfo m) {
            return serialize.invokeVirtualMethod(MethodDescriptor.of(m), valueHandle);
        }
        if (member instanceof FieldInfo f) {
            return serialize.readInstanceField(FieldDescriptor.of(f), valueHandle);
        }
        throw new UnsupportedOperationException("Unknown member type: " + member.getClass());
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

    private String ucFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private boolean isBooleanType(String type) {
        return type.equals("boolean") || type.equals("java.lang.Boolean");
    }

    private boolean vetoedClassName(String className) {
        return className.startsWith("java.") || className.startsWith("jakarta.") || className.startsWith("io.vertx.core.json.");
    }
}
