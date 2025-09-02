package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.VoidType;

import com.fasterxml.jackson.annotation.JsonValue;
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
public class JacksonSerializerFactory extends JacksonCodeGenerator {

    private static final String CLASS_NAME_SUFFIX = "$quarkusjacksonserializer";
    private static final String SUPER_CLASS_NAME = StdSerializer.class.getName();
    private static final String JSON_GEN_CLASS_NAME = JsonGenerator.class.getName();
    private static final String SER_STRINGS_CLASS_NAME = "SerializedStrings$quarkusjacksonserializer";

    private final Map<String, Set<String>> generatedFields = new HashMap<>();

    public JacksonSerializerFactory(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            IndexView jandexIndex) {
        super(generatedClassBuildItemBuildProducer, jandexIndex);
    }

    @Override
    public Collection<String> create(Collection<ClassInfo> classInfos) {
        Collection<String> createdClasses = super.create(classInfos);
        createFieldNamesClass();
        return createdClasses;
    }

    private void createFieldNamesClass() {
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

    @Override
    protected String getSuperClassName() {
        return SUPER_CLASS_NAME;
    }

    @Override
    protected String getClassSuffix() {
        return CLASS_NAME_SUFFIX;
    }

    @Override
    protected boolean createSerializationMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName) {
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

        var jsonValueFieldSpecs = jsonValueFieldSpecs(classInfo);
        if (jsonValueFieldSpecs == null) {
            return false;
        }

        SerializationContext ctx = new SerializationContext(serialize, beanClassName);

        if (jsonValueFieldSpecs.isPresent()) {
            serializeJsonValue(ctx, serialize, jsonValueFieldSpecs.get());
            return true;
        }

        // jsonGenerator.writeStartObject();
        MethodDescriptor writeStartObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeStartObject", "void");
        serialize.invokeVirtualMethod(writeStartObject, ctx.jsonGenerator);

        Set<String> serializedFields = new HashSet<>();
        boolean valid = serializeObjectData(classInfo, classCreator, serialize, ctx, serializedFields);

        // jsonGenerator.writeEndObject();
        MethodDescriptor writeEndObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeEndObject", "void");
        serialize.invokeVirtualMethod(writeEndObject, ctx.jsonGenerator);

        if (serializedFields.isEmpty()) {
            throwExceptionForEmptyBean(beanClassName, serialize, ctx.jsonGenerator);
        }

        classCreator.getMethodCreator("<clinit>", void.class).setModifiers(ACC_STATIC).returnVoid();

        return valid;
    }

    private Optional<FieldSpecs> jsonValueFieldSpecs(ClassInfo classInfo) {
        var jsonValueAnnotationFound = classInfo.hasAnnotation(JsonValue.class);
        if (!jsonValueAnnotationFound) {
            //  Early exit;don't generate reflection-free serializer
            //  based on JsonValue
            return Optional.empty();
        }
        var jsonValueMethodFieldSpecs = classInfo.methods().stream()
                .filter(mi -> mi.annotation(JsonValue.class) != null)
                .filter(this::isJsonValueMethod).findFirst().map(FieldSpecs::new);
        var jsonValueFieldFieldSpecs = classInfo.fields().stream()
                .filter(f -> f.annotation(JsonValue.class) != null)
                .filter(this::isJsonValueField)
                .findFirst().map(FieldSpecs::new);

        if (jsonValueFieldFieldSpecs.isPresent()) {
            return jsonValueMethodFieldSpecs.isPresent() ? null : jsonValueFieldFieldSpecs;
        }
        //  If none valid reflection-free JsonValue annotated target has been found,but
        //  a non-public element annotated is present,just use standard Jackson
        //  serializer
        if (jsonValueMethodFieldSpecs.isEmpty() && jsonValueAnnotationFound) {
            return null;
        }
        return jsonValueMethodFieldSpecs;
    }

    private void serializeJsonValue(SerializationContext ctx, MethodCreator bytecode, FieldSpecs jsonValueFieldSpecs) {
        String typeName = jsonValueFieldSpecs.fieldType.name().toString();
        ResultHandle arg = jsonValueFieldSpecs.toValueReaderHandle(bytecode, ctx.valueHandle);
        writeFieldValue(jsonValueFieldSpecs, bytecode, ctx, typeName, arg, null);
    }

    private boolean serializeObjectData(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            SerializationContext ctx, Set<String> serializedFields) {
        return serializeFields(classInfo, classCreator, serialize, ctx, serializedFields) &&
                serializeMethods(classInfo, classCreator, serialize, ctx, serializedFields);
    }

    private boolean serializeFields(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            SerializationContext ctx, Set<String> serializedFields) {
        MethodInfo constructor = findConstructor(classInfo).orElse(null);

        for (FieldInfo fieldInfo : classFields(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(classInfo, constructor, fieldInfo);
            if (fieldSpecs != null && serializedFields.add(fieldSpecs.jsonName)) {
                if (fieldSpecs.isIgnoredField()) {
                    continue;
                }
                if (fieldSpecs.hasUnknownAnnotation()) {
                    return false;
                }
                writeField(classInfo, fieldSpecs, writeFieldBranch(classCreator, serialize, fieldSpecs), ctx);
            }
        }
        return true;
    }

    private boolean serializeMethods(ClassInfo classInfo, ClassCreator classCreator, MethodCreator serialize,
            SerializationContext ctx, Set<String> serializedFields) {
        for (MethodInfo methodInfo : classMethods(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo);
            if (fieldSpecs != null && serializedFields.add(fieldSpecs.jsonName)) {
                if (fieldSpecs.isIgnoredField()) {
                    continue;
                }
                if (fieldSpecs.hasUnknownAnnotation()) {
                    return false;
                }
                writeField(classInfo, fieldSpecs, serialize, ctx);
            }
        }
        return true;
    }

    private FieldSpecs fieldSpecsFromMethod(MethodInfo methodInfo) {
        return !Modifier.isStatic(methodInfo.flags()) && isGetterMethod(methodInfo) ? new FieldSpecs(methodInfo) : null;
    }

    private boolean isJsonValueMethod(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && methodInfo.parametersCount() == 0
                && !methodInfo.returnType().equals(VoidType.VOID);
    }

    private boolean isJsonValueField(FieldInfo fieldInfo) {
        return Modifier.isPublic(fieldInfo.flags()) && !Modifier.isStatic(fieldInfo.flags());
    }

    private boolean isGetterMethod(MethodInfo methodInfo) {
        String methodName = methodInfo.name();
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && methodInfo.parametersCount() == 0
                && (methodName.startsWith("get") || methodName.startsWith("is"));
    }

    private void writeField(ClassInfo classInfo, FieldSpecs fieldSpecs, BytecodeCreator bytecode, SerializationContext ctx) {
        String pkgName = classInfo.name().packagePrefixName().toString();
        generatedFields.computeIfAbsent(pkgName, pkg -> new HashSet<>()).add(fieldSpecs.jsonName);

        ResultHandle arg = fieldSpecs.toValueReaderHandle(bytecode, ctx.valueHandle);
        bytecode = checkInclude(bytecode, ctx, arg);

        String typeName = fieldSpecs.fieldType.name().toString();
        writeFieldValue(fieldSpecs, bytecode, ctx, typeName, arg, pkgName);
    }

    private void writeFieldValue(FieldSpecs fieldSpecs, BytecodeCreator bytecode, SerializationContext ctx, String typeName,
            ResultHandle arg, String pkgName) {
        String primitiveMethodName = writeMethodForPrimitiveFields(typeName);

        if (primitiveMethodName != null) {
            BytecodeCreator primitiveBytecode = JacksonSerializationUtils.isBoxedPrimitive(typeName)
                    ? bytecode.ifNotNull(arg).trueBranch()
                    : bytecode;

            if (pkgName != null) {
                writeFieldName(fieldSpecs, primitiveBytecode, ctx.jsonGenerator, pkgName);
            }

            MethodDescriptor primitiveWriter = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, primitiveMethodName, "void",
                    fieldSpecs.writtenType());
            primitiveBytecode.invokeVirtualMethod(primitiveWriter, ctx.jsonGenerator, arg);

        } else {
            if (pkgName != null) {
                registerTypeToBeGenerated(fieldSpecs.fieldType, typeName);
                writeFieldName(fieldSpecs, bytecode, ctx.jsonGenerator, pkgName);
            }

            MethodDescriptor writeMethod = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writePOJO",
                    void.class, Object.class);
            bytecode.invokeVirtualMethod(writeMethod, ctx.jsonGenerator, arg);
        }
    }

    private static BytecodeCreator checkInclude(BytecodeCreator bytecode, SerializationContext ctx, ResultHandle arg) {
        MethodDescriptor shouldSerialize = MethodDescriptor.ofMethod(JacksonMapperUtil.SerializationInclude.class,
                "shouldSerialize",
                boolean.class, Object.class);
        ResultHandle included = bytecode.invokeVirtualMethod(shouldSerialize, ctx.includeHandle, arg);
        return bytecode.ifTrue(included).trueBranch();
    }

    private static void writeFieldName(FieldSpecs fieldSpecs, BytecodeCreator bytecode, ResultHandle jsonGenerator,
            String pkgName) {
        MethodDescriptor writeFieldName = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeFieldName", void.class,
                SerializableString.class);
        ResultHandle serStringHandle = bytecode.readStaticField(
                FieldDescriptor.of(pkgName + "." + SER_STRINGS_CLASS_NAME, fieldSpecs.jsonName,
                        SerializedString.class.getName()));
        bytecode.invokeVirtualMethod(writeFieldName, jsonGenerator, serStringHandle);
    }

    private String writeMethodForPrimitiveFields(String typeName) {
        return switch (typeName) {
            case "java.lang.String", "char", "java.lang.Character" -> "writeString";
            case "short", "java.lang.Short", "int", "java.lang.Integer", "long", "java.lang.Long", "float", "java.lang.Float",
                    "double", "java.lang.Double" ->
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

    private record SerializationContext(ResultHandle valueHandle, ResultHandle jsonGenerator, ResultHandle serializerProvider,
            ResultHandle includeHandle) {
        SerializationContext(MethodCreator serialize, String beanClassName) {
            this(valueHandle(serialize, beanClassName), serialize.getMethodParam(1), serialize.getMethodParam(2),
                    includeHandle(serialize));
        }

        private static ResultHandle valueHandle(MethodCreator serialize, String beanClassName) {
            return serialize.checkCast(serialize.getMethodParam(0), beanClassName);
        }

        private static ResultHandle includeHandle(MethodCreator serialize) {
            MethodDescriptor decodeInclude = MethodDescriptor.ofMethod(JacksonMapperUtil.SerializationInclude.class, "decode",
                    JacksonMapperUtil.SerializationInclude.class, Object.class, SerializerProvider.class);
            return serialize.invokeStaticMethod(decodeInclude, serialize.getMethodParam(0), serialize.getMethodParam(2));
        }
    }
}