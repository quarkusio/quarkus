package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.IOException;
import java.lang.reflect.Modifier;
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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.Switch;
import io.quarkus.gizmo.TryBlock;
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
 *     static final String[] TRANSLATABLE_FIELD_NAMES = new String[] { "firstName", "lastName", "address", "age" };
 *
 *     public Person$quarkusjacksondeserializer() {
 *         super(Person.class);
 *     }
 *
 *     public Object deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JacksonException {
 *         PropertyNamingStrategy propertyNamingStrategy = context.getConfig().getPropertyNamingStrategy();
 *         Map<String, String> translatedFields = propertyNamingStrategy == null ? null
 *                 : JacksonMapperUtil.buildReverseNameIndex(propertyNamingStrategy, TRANSLATABLE_FIELD_NAMES);
 *
 *         JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
 *         Person person = new Person();
 *         Iterator fields = jsonNode.fields();
 *
 *         while (fields.hasNext()) {
 *             Entry entry = (Entry) fields.next();
 *             JsonNode jsonValue = (JsonNode) entry.getValue();
 *             if (!jsonValue.isNull()) {
 *                 String key = (String) entry.getKey();
 *                 Object fieldName = translatedFields == null ? key : translatedFields.getOrDefault(key, key);
 *                 switch (fieldName) {
 *                     case "firstName":
 *                         person.setFirstName(jsonNode.asText());
 *                         break;
 *                     case "familyName":
 *                         person.setLastName(jsonNode.asText());
 *                         break;
 *                     case "age":
 *                         person.setAge(jsonNode.asInt());
 *                         break;
 *                     case "address":
 *                         person.setAddress(context.readTreeAsValue(jsonNode, Address.class));
 *                         break;
 *                     default:
 *                         if (context.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
 *                             throw new JsonMappingException("Unrecognized field \"" + fieldName + "\"");
 *                         }
 *                 }
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
 *                 default:
 *                     if (context.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
 *                         throw new JsonMappingException("Unrecognized field \"" + field + "\"");
 *                     }
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
        ResultHandle strategyHandle = getStrategyHandle(deserialize);
        PropertyNamingStrategy namingStrategy = getNamingStrategy(classInfo);
        Set<String> translatableNames = collectTranslatableFieldNames(classInfo, ctor, namingStrategy);
        ResultHandle reverseIndexHandle = null;
        if (!translatableNames.isEmpty()) {
            generateTranslatableFieldNamesStaticField(classCreator, translatableNames);
            reverseIndexHandle = buildReverseIndexHandle(deserialize, classCreator, strategyHandle);
        }
        ResultHandle activeViewHandle = deserialize.invokeVirtualMethod(
                ofMethod(DeserializationContext.class, "getActiveView", Class.class),
                deserialize.getMethodParam(1));
        DeserializationData deserData = new DeserializationData(classInfo, ctor, classCreator, deserialize,
                getJsonNode(deserialize), parseTypeParameters(classInfo, classCreator), new HashSet<>(),
                namingStrategy, strategyHandle, reverseIndexHandle, activeViewHandle);

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

    private static ResultHandle getStrategyHandle(MethodCreator deserialize) {
        ResultHandle deserCtx = deserialize.getMethodParam(1);
        ResultHandle config = deserialize.invokeVirtualMethod(
                ofMethod(DeserializationContext.class, "getConfig", DeserializationConfig.class), deserCtx);
        return deserialize.invokeVirtualMethod(
                ofMethod(DeserializationConfig.class, "getPropertyNamingStrategy", PropertyNamingStrategy.class), config);
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
            FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(paramInfo, deserData.namingStrategy);
            deserData.constructorFields.add(fieldSpecs.jsonName);
            for (String alias : fieldSpecs.aliases) {
                deserData.constructorFields.add(alias);
            }

            ResultHandle fieldValue = lookupJsonField(deserData.methodCreator, deserData.jsonNode, fieldSpecs,
                    deserData.strategyHandle);

            params[i++] = readValueFromJson(deserData.classCreator, deserData.methodCreator,
                    deserData.methodCreator.getMethodParam(1), fieldSpecs, deserData.typeParametersIndex, fieldValue);
        }
        return deserData.methodCreator.newInstance(deserData.constructor, params);
    }

    /**
     * Looks up a field value from a JSON node by its primary name, falling back to any @JsonAlias names.
     * For fields without an explicit JSON name, the lookup name is dynamically resolved through
     * the ObjectMapper's PropertyNamingStrategy if one is configured at runtime.
     */
    private static ResultHandle lookupJsonField(BytecodeCreator bytecode, ResultHandle jsonNode, FieldSpecs fieldSpecs,
            ResultHandle strategyHandle) {
        ResultHandle lookupName = resolveLookupName(bytecode, fieldSpecs, strategyHandle);

        if (fieldSpecs.aliases.length == 0) {
            return bytecode.invokeVirtualMethod(
                    ofMethod(JsonNode.class, "get", JsonNode.class, String.class), jsonNode, lookupName);
        }

        AssignableResultHandle fieldValue = bytecode.createVariable(JsonNode.class);
        bytecode.assign(fieldValue, bytecode.invokeVirtualMethod(
                ofMethod(JsonNode.class, "get", JsonNode.class, String.class), jsonNode, lookupName));

        for (String alias : fieldSpecs.aliases) {
            BytecodeCreator fallback = bytecode.ifNull(fieldValue).trueBranch();
            fallback.assign(fieldValue, fallback.invokeVirtualMethod(
                    ofMethod(JsonNode.class, "get", JsonNode.class, String.class), jsonNode,
                    fallback.load(alias)));
        }

        return fieldValue;
    }

    /**
     * Resolves the JSON field name to use for looking up a value in the JSON node.
     * For fields with an explicit name (@JsonProperty or @JsonNaming), uses that name directly.
     * For fields without an explicit name, dynamically translates through the runtime
     * PropertyNamingStrategy if one is configured on the ObjectMapper.
     */
    private static ResultHandle resolveLookupName(BytecodeCreator bytecode, FieldSpecs fieldSpecs,
            ResultHandle strategyHandle) {
        if (fieldSpecs.hasExplicitJsonName) {
            return bytecode.load(fieldSpecs.jsonName);
        }
        // strategy != null ? strategy.nameForField(null, null, fieldName) : fieldName
        AssignableResultHandle resolvedName = bytecode.createVariable(String.class);
        bytecode.assign(resolvedName, bytecode.load(fieldSpecs.fieldName));
        BytecodeCreator hasStrategy = bytecode.ifNotNull(strategyHandle).trueBranch();
        hasStrategy.assign(resolvedName, hasStrategy.invokeVirtualMethod(
                ofMethod(PropertyNamingStrategy.class, "nameForField", String.class,
                        MapperConfig.class, AnnotatedField.class, String.class),
                strategyHandle, hasStrategy.loadNull(), hasStrategy.loadNull(),
                hasStrategy.load(fieldSpecs.fieldName)));
        return resolvedName;
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

        ResultHandle fieldName = translateFieldName(deserData, loopCreator, mapEntry);
        Switch.StringSwitch strSwitch = loopCreator.stringSwitch(fieldName);

        // save constructor field names before deserializeFields modifies the set
        Set<String> ctorFields = Set.copyOf(deserData.constructorFields);

        Set<String> ignoredProperties = getIgnoredProperties(deserData.classInfo);
        deserData.constructorFields.addAll(ignoredProperties);

        ResultHandle deserializationContext = deserData.methodCreator.getMethodParam(1);
        boolean result = deserializeFields(deserData, deserializationContext, objHandle, fieldValue,
                deserData.constructorFields, strSwitch);

        // add no-op cases for constructor fields (already deserialized in createDeserializedObject)
        for (String ctorField : ctorFields) {
            strSwitch.caseOf(ctorField, bytecode -> {
            });
        }

        MethodInfo anySetterMethod = findAnySetterMethod(deserData.classInfo);
        handleUnknownFields(deserData, ignoredProperties, ctorFields, strSwitch, deserializationContext, fieldName,
                fieldValue, objHandle, anySetterMethod);
        return result;
    }

    private static void handleUnknownFields(DeserializationData deserData, Set<String> ignoredProperties,
            Set<String> ctorFields, Switch.StringSwitch strSwitch, ResultHandle deserializationContext,
            ResultHandle fieldName, ResultHandle fieldValue, ResultHandle objHandle, MethodInfo anySetterMethod) {
        // add no-op cases for explicitly ignored properties
        for (String ignoredProp : ignoredProperties) {
            if (!ctorFields.contains(ignoredProp)) {
                strSwitch.caseOf(ignoredProp, bytecode -> {
                });
            }
        }

        if (anySetterMethod != null) {
            strSwitch.defaultCase(bytecode -> {
                ResultHandle deserializedValue = bytecode.invokeVirtualMethod(
                        ofMethod(DeserializationContext.class, "readTreeAsValue", Object.class, JsonNode.class, Class.class),
                        deserializationContext, fieldValue,
                        bytecode.loadClass(anySetterMethod.parameterType(1).name().toString()));
                ResultHandle castedFieldName = bytecode.checkCast(fieldName, String.class);
                if (anySetterMethod.declaringClass().isInterface()) {
                    bytecode.invokeInterfaceMethod(anySetterMethod, objHandle, castedFieldName, deserializedValue);
                } else {
                    bytecode.invokeVirtualMethod(anySetterMethod, objHandle, castedFieldName, deserializedValue);
                }
            });
        } else if (shouldIgnoreUnknownProperties(deserData.classInfo)) {
            strSwitch.defaultCase(bytecode -> {
            });
        } else {
            strSwitch.defaultCase(bytecode -> {
                ResultHandle failOnUnknown = bytecode.invokeVirtualMethod(
                        ofMethod(DeserializationContext.class, "isEnabled", boolean.class, DeserializationFeature.class),
                        deserializationContext,
                        bytecode.readStaticField(FieldDescriptor.of(DeserializationFeature.class,
                                "FAIL_ON_UNKNOWN_PROPERTIES", DeserializationFeature.class)));
                BytecodeCreator trueBranch = bytecode.ifTrue(failOnUnknown).trueBranch();
                ResultHandle message = trueBranch.invokeVirtualMethod(
                        ofMethod(String.class, "concat", String.class, String.class),
                        trueBranch.load("Unrecognized field \""),
                        trueBranch.invokeVirtualMethod(
                                ofMethod(String.class, "concat", String.class, String.class),
                                trueBranch.checkCast(fieldName, String.class),
                                trueBranch.load("\"")));
                ResultHandle exception = trueBranch.newInstance(
                        MethodDescriptor.ofConstructor(JsonMappingException.class, String.class), message);
                trueBranch.throwException(exception);
            });
        }
    }

    /**
     * Generates bytecode that builds a {@code Map<String, String>} reverse-name index once,
     * before the field iteration loop. The map is {@code null} when no strategy is configured,
     * so per-field lookups are a simple {@code Map.getOrDefault} call.
     */
    private static ResultHandle buildReverseIndexHandle(MethodCreator deserialize, ClassCreator classCreator,
            ResultHandle strategyHandle) {
        AssignableResultHandle reverseIndex = deserialize.createVariable(Map.class);
        deserialize.assign(reverseIndex, deserialize.loadNull());
        BytecodeCreator hasStrategy = deserialize.ifNotNull(strategyHandle).trueBranch();
        ResultHandle namesArray = hasStrategy.readStaticField(
                FieldDescriptor.of(classCreator.getClassName(), TRANSLATABLE_FIELD_NAMES, String[].class));
        hasStrategy.assign(reverseIndex, hasStrategy.invokeStaticMethod(
                ofMethod(JacksonMapperUtil.class, "buildReverseNameIndex", Map.class,
                        PropertyNamingStrategy.class, String[].class),
                strategyHandle, namesArray));
        return reverseIndex;
    }

    private static ResultHandle translateFieldName(DeserializationData deserData, BytecodeCreator fieldReader,
            ResultHandle mapEntry) {
        ResultHandle rawFieldName = fieldReader
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getKey", Object.class), mapEntry);

        if (deserData.reverseIndexHandle == null) {
            return rawFieldName;
        }

        // Reverse-translate the incoming field name through the pre-built index (O(1) per field)
        AssignableResultHandle resolved = fieldReader.createVariable(Object.class);
        fieldReader.assign(resolved, rawFieldName);
        BytecodeCreator hasIndex = fieldReader.ifNotNull(deserData.reverseIndexHandle).trueBranch();
        hasIndex.assign(resolved, hasIndex.invokeInterfaceMethod(
                ofMethod(Map.class, "getOrDefault", Object.class, Object.class, Object.class),
                deserData.reverseIndexHandle, rawFieldName, rawFieldName));
        return resolved;
    }

    private Set<String> collectTranslatableFieldNames(ClassInfo classInfo, MethodInfo constructor,
            PropertyNamingStrategy namingStrategy) {
        Set<String> names = new HashSet<>();
        if (constructor.parametersCount() > 0) {
            for (MethodParameterInfo paramInfo : constructor.parameters()) {
                FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(paramInfo, namingStrategy);
                if (!fieldSpecs.hasExplicitJsonName) {
                    names.add(fieldSpecs.fieldName);
                }
            }
        }
        for (FieldInfo fieldInfo : classFields(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(classInfo, constructor, fieldInfo, namingStrategy);
            if (fieldSpecs != null && !fieldSpecs.hasExplicitJsonName && !fieldSpecs.isIgnoredField()) {
                names.add(fieldSpecs.fieldName);
            }
        }
        for (MethodInfo methodInfo : classMethods(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo, namingStrategy);
            if (fieldSpecs != null && !fieldSpecs.hasExplicitJsonName && !fieldSpecs.isIgnoredField()) {
                names.add(fieldSpecs.fieldName);
            }
        }
        return names;
    }

    private static void generateTranslatableFieldNamesStaticField(ClassCreator classCreator, Set<String> translatableNames) {
        MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class).setModifiers(ACC_STATIC);
        ResultHandle namesArray = clinit.newArray(String.class, translatableNames.size());
        int i = 0;
        for (String name : translatableNames) {
            clinit.writeArrayValue(namesArray, i++, clinit.load(name));
        }
        FieldCreator fieldCreator = classCreator
                .getFieldCreator(TRANSLATABLE_FIELD_NAMES, String[].class.getName())
                .setModifiers(ACC_STATIC | ACC_FINAL);
        clinit.writeStaticField(fieldCreator.getFieldDescriptor(), namesArray);
        clinit.returnVoid();
    }

    private BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }

    private Map<String, Integer> parseTypeParameters(ClassInfo classInfo, ClassCreator classCreator) {
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (typeParameters.isEmpty()) {
            return Map.of();
        }

        createContextualMethod(classCreator);

        Map<String, Integer> typeParametersIndex = new HashMap<>();
        int index = 0;
        for (TypeVariable typeParameter : typeParameters) {
            typeParametersIndex.put(typeParameter.identifier(), index++);
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
            deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch,
                    fieldSpecsFromField(deserData.classInfo, deserData.constructor, fieldInfo, deserData.namingStrategy),
                    valid);
        }

        for (MethodInfo methodInfo : classMethods(deserData.classInfo)) {
            deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch, fieldSpecsFromMethod(methodInfo, deserData.namingStrategy), valid);
        }

        return valid.get();
    }

    private void deserializeFieldSpecs(DeserializationData deserData, ResultHandle deserializationContext,
            ResultHandle objHandle, ResultHandle fieldValue, Set<String> deserializedFields, Switch.StringSwitch strSwitch,
            FieldSpecs fieldSpecs, AtomicBoolean valid) {
        if (fieldSpecs != null && deserializedFields.add(fieldSpecs.jsonName)) {
            if (fieldSpecs.isIgnoredField()) {
                return;
            }
            strSwitch.caseOf(fieldSpecs.jsonName,
                    bytecode -> valid.compareAndSet(true, deserializeField(deserData, bytecode, objHandle,
                            fieldValue, fieldSpecs, deserializationContext)));
            for (String alias : fieldSpecs.aliases) {
                strSwitch.caseOf(alias,
                        bytecode -> valid.compareAndSet(true, deserializeField(deserData, bytecode, objHandle,
                                fieldValue, fieldSpecs, deserializationContext)));
            }
        }
    }

    private boolean deserializeField(DeserializationData deserData, BytecodeCreator bytecode,
            ResultHandle objHandle, ResultHandle fieldValue, FieldSpecs fieldSpecs,
            ResultHandle deserializationContext) {
        bytecode = deserializeViewClasses(deserData, bytecode, fieldSpecs);

        boolean isBasicType = JacksonSerializationUtils.isBasicJsonType(fieldSpecs.fieldType);

        // For non-basic types (objects, collections, boxed primitives, etc.), wrap in try-catch
        // to enrich any MismatchedInputException with the field path context.
        // This ensures the exception mapper can report the object name and attribute name.
        BytecodeCreator effectiveBytecode = bytecode;
        TryBlock tryBlock = null;
        if (!isBasicType) {
            tryBlock = bytecode.tryBlock();
            effectiveBytecode = tryBlock;
        }

        ResultHandle valueHandle = readValueFromJson(deserData.classCreator, effectiveBytecode, deserializationContext,
                fieldSpecs, deserData.typeParametersIndex, fieldValue);
        if (valueHandle == null) {
            return false;
        }
        writeValueToObject(deserData.classInfo, objHandle, fieldSpecs, effectiveBytecode,
                fieldSpecs.toValueWriterHandle(effectiveBytecode, valueHandle));

        if (tryBlock != null) {
            CatchBlockCreator catchBlock = tryBlock.addCatch(MismatchedInputException.class);
            ResultHandle exception = catchBlock.getCaughtException();
            catchBlock.invokeVirtualMethod(
                    ofMethod(JsonMappingException.class, "prependPath", void.class, Object.class, String.class),
                    exception, objHandle, catchBlock.load(fieldSpecs.jsonName));
            catchBlock.throwException(exception);
        }

        return true;
    }

    private static BytecodeCreator deserializeViewClasses(DeserializationData deserData, BytecodeCreator bytecode,
            FieldSpecs fieldSpecs) {
        String[] viewClasses = fieldSpecs.viewClasses();
        if (viewClasses != null) {
            ResultHandle viewClassesArray = bytecode.newArray(Class.class, viewClasses.length);
            for (int i = 0; i < viewClasses.length; i++) {
                bytecode.writeArrayValue(viewClassesArray, i, bytecode.loadClass(viewClasses[i]));
            }
            MethodDescriptor isViewIncluded = ofMethod(JacksonMapperUtil.class, "isViewIncluded",
                    boolean.class, Class.class, Class[].class);
            ResultHandle included = bytecode.invokeStaticMethod(isViewIncluded, deserData.activeViewHandle(),
                    viewClassesArray);
            bytecode = bytecode.ifTrue(included).trueBranch();
        }
        return bytecode;
    }

    private MethodInfo findAnySetterMethod(ClassInfo classInfo) {
        for (MethodInfo method : classMethods(classInfo)) {
            if (method.hasAnnotation(JsonAnySetter.class)
                    && method.parametersCount() == 2
                    && !Modifier.isStatic(method.flags())) {
                return method;
            }
        }
        return null;
    }

    private FieldSpecs fieldSpecsFromMethod(MethodInfo methodInfo, PropertyNamingStrategy namingStrategy) {
        return isSetterMethod(methodInfo) ? new FieldSpecs(null, null, methodInfo, namingStrategy) : null;
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
                Integer parameterIndex = typeParametersIndex.get(fieldType.asTypeVariable().identifier());
                if (parameterIndex == null) {
                    yield null;
                }
                FieldDescriptor valueTypesField = FieldDescriptor.of(classCreator.getClassName(), "valueTypes",
                        JavaType[].class);
                ResultHandle valueTypes = bytecode.readInstanceField(valueTypesField, bytecode.getThis());
                yield bytecode.readArrayValue(valueTypes, parameterIndex);
            }
            case LIST, SET, OPTIONAL -> {
                Type contentType = ((ParameterizedType) fieldType).arguments().get(0);
                MethodDescriptor getTypeFactory = ofMethod(DeserializationContext.class, "getTypeFactory",
                        TypeFactory.class);
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(getTypeFactory, deserializationContext);
                MethodDescriptor constructParametricType = ofMethod(TypeFactory.class,
                        "constructParametricType", JavaType.class, Class.class, Class[].class);
                ResultHandle paramTypes = bytecode.newArray(Class.class, 1);
                bytecode.writeArrayValue(paramTypes, 0, bytecode.loadClass(contentType.name().toString()));
                yield bytecode.invokeVirtualMethod(constructParametricType, typeFactory,
                        bytecode.loadClass(fieldTypeName), paramTypes);
            }
            case MAP -> {
                Type keyType = ((ParameterizedType) fieldType).arguments().get(0);
                Type valueType = ((ParameterizedType) fieldType).arguments().get(1);
                MethodDescriptor getTypeFactory = ofMethod(DeserializationContext.class, "getTypeFactory",
                        TypeFactory.class);
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(getTypeFactory, deserializationContext);
                MethodDescriptor constructParametricType = ofMethod(TypeFactory.class,
                        "constructParametricType", JavaType.class, Class.class, Class[].class);
                ResultHandle paramTypes = bytecode.newArray(Class.class, 2);
                bytecode.writeArrayValue(paramTypes, 0, bytecode.loadClass(keyType.name().toString()));
                bytecode.writeArrayValue(paramTypes, 1, bytecode.loadClass(valueType.name().toString()));
                yield bytecode.invokeVirtualMethod(constructParametricType, typeFactory,
                        bytecode.loadClass(fieldTypeName), paramTypes);
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

        BranchResult isNullNode = isValueNull.falseBranch()
                .ifTrue(isValueNull.falseBranch().invokeVirtualMethod(ofMethod(JsonNode.class, "isNull", boolean.class),
                        valueNode));
        isNullNode.trueBranch().assign(result, JacksonSerializationUtils.getDefaultValue(isNullNode.trueBranch(), fieldType));

        BytecodeCreator isValueNullFalse = isNullNode.falseBranch();

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

    private static final String TRANSLATABLE_FIELD_NAMES = "TRANSLATABLE_FIELD_NAMES";

    private record DeserializationData(ClassInfo classInfo, MethodInfo constructor, ClassCreator classCreator,
            MethodCreator methodCreator,
            ResultHandle jsonNode, Map<String, Integer> typeParametersIndex, Set<String> constructorFields,
            PropertyNamingStrategy namingStrategy, ResultHandle strategyHandle, ResultHandle reverseIndexHandle,
            ResultHandle activeViewHandle) {
    }
}
