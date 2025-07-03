package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.VoidType;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.Switch;
import io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil;

/**
 * Generates an implementation of the Jackson's {@code StdDeserializer} for each class that needs to be deserialized from json.
 * In this way the deserialization process can be performed through the ad-hoc generate deserializer and then without
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
 * it generates the following {@code StdDeserializer} implementation
 *
 * <pre>{@code
 * public class Person$quarkusjacksondeserializer extends StdDeserializer {
 *     public Person$quarkusjacksondeserializer() {
 *         super(Person.class);
 *     }
 *
 *     public Object deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JacksonException {
 *         Person person = new Person();
 *         Iterator iterator = ((JsonNode) jsonParser.getCodec().readTree(jsonParser)).fields();
 *
 *         while (iterator.hasNext()) {
 *             Map.Entry entry = (Map.iterator) var3.next();
 *             String field = (String) entry.getKey();
 *             JsonNode jsonNode = (JsonNode) entry.getValue();
 *             switch (field) {
 *                 case "firstName":
 *                     person.setFirstName(jsonNode.asText());
 *                     break;
 *                 case "familyName":
 *                     person.setLastName(jsonNode.asText());
 *                     break;
 *                 case "age":
 *                     person.setAge(jsonNode.asInt());
 *                     break;
 *                 case "address":
 *                     person.setAddress(context.readTreeAsValue(jsonNode, Address.class));
 *                     break;
 *             }
 *         }
 *
 *         return person;
 *     }
 * }
 * }</pre>
 *
 * Note that in this case also the {@code Address} class has to be deserialized in the same way, and then this factory triggers
 * the generation of a second StdDeserializer also for it. More in general if during the generation of a deserializer for a
 * given class it discovers a non-primitive field of another type for which a deserializer hasn't been generated yet, this
 * factory enqueues a code generation also for that type. The same is valid for both arrays of that type, like
 * {@code Address[]}, and collections, like {@code List&lt;Address&gt}.
 *
 * Also note that this works only if the Java class to be deserialized has an empty constructor, while the generation of
 * this deserializer is skipped in all other cases. In particular this cannot work with records.
 *
 * If the class to be deserialized has one or more generics parameter, the generated deserializer also implements the
 * {@code ContextualDeserializer} interface. For instance for a class like the following
 *
 * <pre>{@code
 * public class DataItem<T> {
 *
 *     private T content;
 *
 *     public T getContent() {
 *         return content;
 *     }
 *
 *     public void setContent(T content) {
 *         this.content = content;
 *     }
 * }
 * }</pre>
 *
 * the corresponding generated deserializer will be
 *
 * <pre>{@code
 * public class DataItem$quarkusjacksondeserializer extends StdDeserializer implements ContextualDeserializer {
 *     private JavaType[] valueTypesmvn clean install;
 *
 *     public DataItem$quarkusjacksondeserializer() {
 *         super(DataItem.class);
 *     }
 *
 *     public Object deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JacksonException {
 *         DataItem dataItem = new DataItem();
 *         Iterator iterator = ((JsonNode) jsonParser.getCodec().readTree(jsonParser)).fields();
 *
 *         while (iterator.hasNext()) {
 *             Map.Entry entry = (Map.iterator) var3.next();
 *             String field = (String) entry.getKey();
 *             JsonNode jsonNode = (JsonNode) entry.getValue();
 *             if (jsonNode.isNull()) {
 *                 continue;
 *             }
 *             switch (field) {
 *                 case "content":
 *                     dataItem.setContent(context.readTreeAsValue(jsonNode, this.valueTypes[0]));
 *                     break;
 *             }
 *         }
 *
 *         return dataItem;
 *     }
 *
 *     public JsonDeserializer createContextual(DeserializationContext context, BeanProperty beanProperty) {
 *         JavaType[] valueTypes = JacksonMapperUtil.getGenericsJavaTypes(context, beanProperty);
 *         DataItem$quarkusjacksondeserializer deserializer = new DataItem$quarkusjacksondeserializer();
 *         deserializer.valueTypes = valueTypes;
 *         return (JsonDeserializer) deserializer;
 *     }
 * }
 * }</pre>
 */
public class JacksonDeserializerFactory extends JacksonCodeGenerator {

    public JacksonDeserializerFactory(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            IndexView jandexIndex) {
        super(generatedClassBuildItemBuildProducer, jandexIndex);
    }

    @Override
    protected String getSuperClassName() {
        return StdDeserializer.class.getName();
    }

    @Override
    protected String getClassSuffix() {
        return "$quarkusjacksondeserializer";
    }

    protected String[] getInterfacesNames(ClassInfo classInfo) {
        return classInfo.typeParameters().isEmpty() ? new String[0] : new String[] { ContextualDeserializer.class.getName() };
    }

    @Override
    protected boolean createSerializationMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName) {
        MethodCreator deserialize = classCreator
                .getMethodCreator("deserialize", Object.class, JsonParser.class, DeserializationContext.class)
                .setModifiers(ACC_PUBLIC)
                .addException(IOException.class)
                .addException(JacksonException.class);

        Optional<MethodInfo> ctorOpt = findConstructor(classInfo);
        if (ctorOpt.isEmpty()) {
            return false;
        }

        MethodInfo ctor = ctorOpt.get();
        DeserializationData deserData = new DeserializationData(classInfo, ctor, classCreator, deserialize,
                getJsonNode(deserialize), parseTypeParameters(classInfo, classCreator), new HashSet<>());

        ResultHandle deserializedHandle = ctor.parametersCount() == 0
                ? deserData.methodCreator.newInstance(MethodDescriptor.ofConstructor(deserData.classInfo.name().toString()))
                : createDeserializedObject(deserData);

        if (deserializedHandle == null) {
            return false;
        }

        boolean valid = deserializeObjectFields(deserData, deserializedHandle);
        deserialize.returnValue(deserializedHandle);
        return valid;
    }

    private static ResultHandle getJsonNode(MethodCreator deserialize) {
        ResultHandle jsonParser = deserialize.getMethodParam(0);
        ResultHandle objectCodec = deserialize
                .invokeVirtualMethod(ofMethod(JsonParser.class, "getCodec", ObjectCodec.class), jsonParser);
        ResultHandle treeNode = deserialize.invokeVirtualMethod(
                ofMethod(ObjectCodec.class, "readTree", TreeNode.class, JsonParser.class), objectCodec,
                jsonParser);
        return deserialize.checkCast(treeNode, JsonNode.class);
    }

    private ResultHandle createDeserializedObject(DeserializationData deserData) {
        ResultHandle[] params = new ResultHandle[deserData.constructor.parameters().size()];
        int i = 0;
        for (MethodParameterInfo paramInfo : deserData.constructor.parameters()) {
            FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(paramInfo);
            deserData.constructorFields.add(fieldSpecs.jsonName);
            ResultHandle fieldValue = deserData.methodCreator.invokeVirtualMethod(
                    ofMethod(JsonNode.class, "get", JsonNode.class, String.class), deserData.jsonNode,
                    deserData.methodCreator.load(fieldSpecs.jsonName));

            params[i++] = readValueFromJson(deserData.classCreator, deserData.methodCreator,
                    deserData.methodCreator.getMethodParam(1), fieldSpecs, deserData.typeParametersIndex, fieldValue);
        }
        return deserData.methodCreator.newInstance(deserData.constructor, params);
    }

    private boolean deserializeObjectFields(DeserializationData deserData, ResultHandle objHandle) {

        ResultHandle fieldsIterator = deserData.methodCreator
                .invokeVirtualMethod(ofMethod(JsonNode.class, "fields", Iterator.class), deserData.jsonNode);
        BytecodeCreator loopCreator = deserData.methodCreator.whileLoop(c -> iteratorHasNext(c, fieldsIterator)).block();
        ResultHandle nextField = loopCreator
                .invokeInterfaceMethod(ofMethod(Iterator.class, "next", Object.class), fieldsIterator);
        ResultHandle mapEntry = loopCreator.checkCast(nextField, Map.Entry.class);
        ResultHandle fieldValue = loopCreator.checkCast(loopCreator
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getValue", Object.class), mapEntry), JsonNode.class);

        BytecodeCreator fieldReader = loopCreator
                .ifTrue(loopCreator.invokeVirtualMethod(ofMethod(JsonNode.class, "isNull", boolean.class), fieldValue))
                .falseBranch();

        ResultHandle fieldName = fieldReader
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getKey", Object.class), mapEntry);
        Switch.StringSwitch strSwitch = fieldReader.stringSwitch(fieldName);

        return deserializeFields(deserData, deserData.methodCreator.getMethodParam(1), objHandle, fieldValue,
                deserData.constructorFields, strSwitch);
    }

    private BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }

    private Map<String, Integer> parseTypeParameters(ClassInfo classInfo, ClassCreator classCreator) {
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (typeParameters.isEmpty()) {
            return null;
        }

        createContextualMethod(classCreator);

        Map<String, Integer> typeParametersIndex = new HashMap<>();
        int index = 0;
        for (TypeVariable typeParameter : typeParameters) {
            typeParametersIndex.put(typeParameter.name().toString(), index++);
        }
        return typeParametersIndex;
    }

    private static void createContextualMethod(ClassCreator classCreator) {
        FieldDescriptor valueTypesField = FieldDescriptor.of(classCreator.getClassName(), "valueTypes", JavaType[].class);
        classCreator.getFieldCreator(valueTypesField);

        MethodCreator createContextual = classCreator
                .getMethodCreator("createContextual", JsonDeserializer.class, DeserializationContext.class, BeanProperty.class)
                .setModifiers(ACC_PUBLIC);

        ResultHandle deserializationContext = createContextual.getMethodParam(0);
        ResultHandle beanProperty = createContextual.getMethodParam(1);
        MethodDescriptor getGenericsJavaTypes = ofMethod(JacksonMapperUtil.class, "getGenericsJavaTypes",
                JavaType[].class, DeserializationContext.class, BeanProperty.class);
        ResultHandle valueTypes = createContextual.invokeStaticMethod(getGenericsJavaTypes, deserializationContext,
                beanProperty);

        ResultHandle deserializer = createContextual.newInstance(MethodDescriptor.ofConstructor(classCreator.getClassName()));
        createContextual.writeInstanceField(valueTypesField, deserializer, valueTypes);
        createContextual.returnValue(deserializer);
    }

    private boolean deserializeFields(DeserializationData deserData, ResultHandle deserializationContext,
            ResultHandle objHandle, ResultHandle fieldValue, Set<String> deserializedFields, Switch.StringSwitch strSwitch) {

        AtomicBoolean valid = new AtomicBoolean(true);

        for (FieldInfo fieldInfo : classFields(deserData.classInfo)) {
            if (!deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch, fieldSpecsFromField(deserData.classInfo, deserData.constructor, fieldInfo),
                    valid))
                return false;
        }

        for (MethodInfo methodInfo : classMethods(deserData.classInfo)) {
            if (!deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch, fieldSpecsFromMethod(methodInfo), valid))
                return false;
        }

        return valid.get();
    }

    private boolean deserializeFieldSpecs(DeserializationData deserData, ResultHandle deserializationContext,
            ResultHandle objHandle, ResultHandle fieldValue, Set<String> deserializedFields, Switch.StringSwitch strSwitch,
            FieldSpecs fieldSpecs, AtomicBoolean valid) {
        if (fieldSpecs != null && deserializedFields.add(fieldSpecs.jsonName)) {
            if (fieldSpecs.isIgnoredField()) {
                return true;
            }
            if (fieldSpecs.hasUnknownAnnotation()) {
                return false;
            }
            strSwitch.caseOf(fieldSpecs.jsonName,
                    bytecode -> valid.compareAndSet(true, deserializeField(deserData, bytecode, objHandle,
                            fieldValue, fieldSpecs, deserializationContext)));
        }
        return true;
    }

    private boolean deserializeField(DeserializationData deserData, BytecodeCreator bytecode,
            ResultHandle objHandle, ResultHandle fieldValue, FieldSpecs fieldSpecs,
            ResultHandle deserializationContext) {
        ResultHandle valueHandle = readValueFromJson(deserData.classCreator, bytecode, deserializationContext, fieldSpecs,
                deserData.typeParametersIndex, fieldValue);
        if (valueHandle == null) {
            return false;
        }
        writeValueToObject(deserData.classInfo, objHandle, fieldSpecs, bytecode,
                fieldSpecs.toValueWriterHandle(bytecode, valueHandle));
        return true;
    }

    private FieldSpecs fieldSpecsFromMethod(MethodInfo methodInfo) {
        return isSetterMethod(methodInfo) ? new FieldSpecs(methodInfo) : null;
    }

    private boolean isSetterMethod(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && methodInfo.returnType() instanceof VoidType && methodInfo.parametersCount() == 1
                && methodInfo.name().startsWith("set");
    }

    private ResultHandle readValueFromJson(ClassCreator classCreator, BytecodeCreator bytecode,
            ResultHandle deserializationContext, FieldSpecs fieldSpecs, Map<String, Integer> typeParametersIndex,
            ResultHandle valueNode) {
        Type fieldType = fieldSpecs.fieldType;
        String fieldTypeName = fieldType.name().toString();
        if (JacksonSerializationUtils.isBasicJsonType(fieldType)) {
            return readValueForPrimitiveFields(bytecode, fieldType, valueNode);
        }

        FieldKind fieldKind = registerTypeToBeGenerated(fieldType, fieldTypeName);
        ResultHandle typeHandle = switch (fieldKind) {
            case TYPE_VARIABLE -> {
                Integer parameterIndex = typeParametersIndex.get(fieldTypeName);
                if (parameterIndex == null) {
                    yield null;
                }
                FieldDescriptor valueTypesField = FieldDescriptor.of(classCreator.getClassName(), "valueTypes",
                        JavaType[].class);
                ResultHandle valueTypes = bytecode.readInstanceField(valueTypesField, bytecode.getThis());
                yield bytecode.readArrayValue(valueTypes, parameterIndex);
            }
            case LIST, SET -> {
                Type listType = ((ParameterizedType) fieldType).arguments().get(0);
                MethodDescriptor getTypeFactory = ofMethod(DeserializationContext.class, "getTypeFactory",
                        TypeFactory.class);
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(getTypeFactory, deserializationContext);
                MethodDescriptor constructCollectionType = ofMethod(TypeFactory.class,
                        "constructCollectionType", CollectionType.class, Class.class, Class.class);
                yield bytecode.invokeVirtualMethod(constructCollectionType, typeFactory,
                        bytecode.loadClass(fieldKind == FieldKind.SET ? HashSet.class : ArrayList.class),
                        bytecode.loadClass(listType.name().toString()));
            }
            case MAP -> {
                Type keyType = ((ParameterizedType) fieldType).arguments().get(0);
                Type valueType = ((ParameterizedType) fieldType).arguments().get(1);
                MethodDescriptor getTypeFactory = ofMethod(DeserializationContext.class, "getTypeFactory",
                        TypeFactory.class);
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(getTypeFactory, deserializationContext);
                MethodDescriptor constructMapType = ofMethod(TypeFactory.class, "constructMapType",
                        MapType.class, Class.class, Class.class, Class.class);
                yield bytecode.invokeVirtualMethod(constructMapType, typeFactory, bytecode.loadClass(HashMap.class),
                        bytecode.loadClass(keyType.name().toString()), bytecode.loadClass(valueType.name().toString()));
            }
            default -> bytecode.loadClass(fieldTypeName);
        };

        if (typeHandle == null) {
            return null;
        }

        MethodDescriptor readTreeAsValue = ofMethod(DeserializationContext.class, "readTreeAsValue",
                Object.class, JsonNode.class, fieldKind.isGeneric() ? JavaType.class : Class.class);
        return bytecode.invokeVirtualMethod(readTreeAsValue, deserializationContext, valueNode, typeHandle);
    }

    private void writeValueToObject(ClassInfo classInfo, ResultHandle objHandle, FieldSpecs fieldSpecs,
            BytecodeCreator bytecode, ResultHandle valueHandle) {
        if (fieldSpecs.isPublicField()) {
            bytecode.writeInstanceField(fieldSpecs.fieldInfo, objHandle, valueHandle);
        } else {
            MethodInfo setterMethod = setterMethodInfo(classInfo, fieldSpecs);
            if (setterMethod != null) {
                if (setterMethod.declaringClass().isInterface()) {
                    bytecode.invokeInterfaceMethod(setterMethod, objHandle, valueHandle);
                } else {
                    bytecode.invokeVirtualMethod(setterMethod, objHandle, valueHandle);
                }
            }
        }
    }

    private MethodInfo setterMethodInfo(ClassInfo classInfo, FieldSpecs fieldSpecs) {
        String methodName = "set" + ucFirst(fieldSpecs.fieldName);
        MethodInfo setter = findMethod(classInfo, methodName, fieldSpecs.fieldType);
        if (setter == null) {
            setter = findMethod(classInfo, fieldSpecs.fieldName, fieldSpecs.fieldType);
        }
        return setter;
    }

    private static ResultHandle readValueForPrimitiveFields(BytecodeCreator bytecode, Type fieldType,
            ResultHandle valueNode) {
        AssignableResultHandle result = bytecode.createVariable(DescriptorUtils.typeToString(fieldType));

        BranchResult isValueNull = bytecode.ifNull(valueNode);
        BytecodeCreator isValueNullTrue = isValueNull.trueBranch();
        isValueNullTrue.assign(result, JacksonSerializationUtils.getDefaultValue(isValueNullTrue, fieldType));

        BytecodeCreator isValueNullFalse = isValueNull.falseBranch();

        ResultHandle convertedValue = switch (fieldType.name().toString()) {
            case "java.lang.String" -> isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asText", String.class),
                    valueNode);
            case "char", "java.lang.Character" -> isValueNullFalse.invokeVirtualMethod(
                    ofMethod(String.class, "charAt", char.class, int.class),
                    isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asText", String.class), valueNode),
                    isValueNullFalse.load(0));
            case "short", "java.lang.Short" -> isValueNullFalse
                    .convertPrimitive(
                            isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asInt", int.class), valueNode),
                            short.class);
            case "int" ->
                isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asInt", int.class),
                        valueNode);
            case "java.lang.Integer" ->
                isValueNullFalse.invokeStaticMethod(ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                        isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asInt", int.class),
                                valueNode));
            case "long", "java.lang.Long" ->
                isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asLong", long.class),
                        valueNode);
            case "float", "java.lang.Float" -> isValueNullFalse
                    .convertPrimitive(
                            isValueNullFalse.invokeVirtualMethod(ofMethod(JsonNode.class, "asDouble", double.class), valueNode),
                            float.class);
            case "double", "java.lang.Double" -> isValueNullFalse
                    .invokeVirtualMethod(ofMethod(JsonNode.class, "asDouble", double.class), valueNode);
            case "boolean", "java.lang.Boolean" -> isValueNullFalse
                    .invokeVirtualMethod(ofMethod(JsonNode.class, "asBoolean", boolean.class), valueNode);
            default -> throw new IllegalStateException("Type " + fieldType + " should be handled by the switch");
        };

        isValueNullFalse.assign(result, convertedValue);

        return result;
    }

    @Override
    protected boolean shouldGenerateCodeFor(ClassInfo classInfo) {
        return super.shouldGenerateCodeFor(classInfo) && classInfo.hasNoArgsConstructor();
    }

    private record DeserializationData(ClassInfo classInfo, MethodInfo constructor, ClassCreator classCreator,
            MethodCreator methodCreator,
            ResultHandle jsonNode, Map<String, Integer> typeParametersIndex, Set<String> constructorFields) {
    }
}
