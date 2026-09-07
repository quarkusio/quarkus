package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.VoidType;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

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
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.sym.SimpleNameMatcher;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.type.TypeFactory;

/**
 * Generates an implementation of the Jackson's {@code StdDeserializer} for each class that needs to be deserialized from json.
 * In this way the deserialization process can be performed through the ad-hoc generate deserializer and then without
 * any use of reflection.
 *
 * <p>
 * When the class has a no-arg constructor, the generated deserializer uses a <b>streaming</b> approach: it reads
 * field names directly from the {@link JsonParser} via {@code nextName()} and dispatches them with a
 * {@link PropertyNameMatcher} that maps each name to an integer index, then an {@code if/else if} chain routes
 * to the corresponding setter, avoiding the intermediate {@code JsonNode} tree entirely. Primitive values are read
 * with coercion-safe parser methods ({@code getValueAsInt()}, {@code getText()}, etc.) while complex types delegate
 * to {@code DeserializationContext.readValue(JsonParser, ...)}. For classes that require constructor-based
 * deserialization (e.g. {@code @JsonCreator}, records), the factory falls back to the tree-based approach that
 * materializes the full {@code JsonNode} first.
 *
 * <p>
 * For instance for a pojo like this
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
 * it generates the following streaming {@code StdDeserializer} implementation
 *
 * <pre>{@code
 * public class Person$quarkusjacksondeserializer extends StdDeserializer {
 *     static final String[] TRANSLATABLE_FIELD_NAMES = new String[] { "firstName", "lastName", "address", "age" };
 *     static final PropertyNameMatcher FIELD_MATCHER = SimpleNameMatcher.construct(
 *             Locale.getDefault(), List.of("firstName", "familyName", "age", "address"));
 *
 *     public Person$quarkusjacksondeserializer() {
 *         super(Person.class);
 *     }
 *
 *     public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
 *         PropertyNamingStrategy strategy = ctxt.getConfig().getPropertyNamingStrategy();
 *
 *         Person person = new Person();
 *         String name;
 *         while ((name = p.nextName()) != null) {
 *             p.nextToken();
 *             int ix = JacksonMapperUtil.matchFieldName(FIELD_MATCHER, name, strategy, TRANSLATABLE_FIELD_NAMES);
 *             if (ix == 0) {
 *                 person.setFirstName(p.getText());
 *             } else if (ix == 1) {
 *                 person.setLastName(p.getText());
 *             } else if (ix == 2) {
 *                 person.setAge(p.getValueAsInt());
 *             } else if (ix == 3) {
 *                 person.setAddress((Address) ctxt.readValue(p, Address.class));
 *             } else {
 *                 if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
 *                     throw UnrecognizedPropertyException.from(p, Person.class, name, null);
 *                 }
 *                 p.skipChildren();
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
 * <p>
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
 *     private JavaType[] valueTypes;
 *     static final PropertyNameMatcher FIELD_MATCHER = SimpleNameMatcher.construct(
 *             Locale.getDefault(), List.of("content"));
 *
 *     public DataItem$quarkusjacksondeserializer() {
 *         super(DataItem.class);
 *     }
 *
 *     public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
 *         DataItem dataItem = new DataItem();
 *         String name;
 *         while ((name = p.nextName()) != null) {
 *             p.nextToken();
 *             int ix = FIELD_MATCHER.matchName(name);
 *             if (ix == 0) {
 *                 dataItem.setContent(ctxt.readValue(p, this.valueTypes[0]));
 *             } else {
 *                 if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
 *                     throw UnrecognizedPropertyException.from(p, DataItem.class, name, null);
 *                 }
 *                 p.skipChildren();
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

    /**
     * Determines whether the streaming deserialization path can be used for the given class.
     * Streaming reads field names directly from the {@link tools.jackson.core.JsonParser} token stream,
     * avoiding the intermediate {@code JsonNode} tree. It requires:
     * <ul>
     * <li>A no-arg constructor — parameterized constructors need all values upfront, which
     * requires materializing the full tree first since JSON field order is not guaranteed.</li>
     * <li>Not {@code @JsonFormat(shape = ARRAY)} — array-shaped beans are positional, not
     * field-name-driven, so the name-matching dispatch does not apply.</li>
     * <li>Not a {@link java.util.Map} subclass — maps put every key/value pair directly
     * rather than routing through per-property setters.</li>
     * <li>No {@code @JsonUnwrapped} fields — unwrapped fields flatten nested object properties
     * into the parent, requiring the full tree to reassemble them before deserialization.</li>
     * </ul>
     */
    private boolean canUseStreaming(ClassInfo classInfo, MethodInfo ctor, String beanClassName) {
        if (ctor.parametersCount() > 0) {
            return false;
        }
        if (isClassFormatShapeArray(classInfo)) {
            return false;
        }
        if (isAssignableTo(beanClassName, MAP_NAME)) {
            return false;
        }
        PropertyNamingStrategy namingStrategy = getNamingStrategy(classInfo);
        for (FieldInfo fieldInfo : classFields(classInfo)) {
            FieldSpecs fs = fieldSpecsFromField(classInfo, ctor, fieldInfo, namingStrategy);
            if (fs != null && fs.isUnwrapped()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean discoverFieldTypes(ClassInfo classInfo, String beanClassName, Map<String, Type> typeBindings) {
        Optional<MethodInfo> ctorOpt = findConstructor(classInfo);
        if (ctorOpt.isEmpty()) {
            return false;
        }

        if (!isClassFormatShapeArray(classInfo) && isAssignableTo(beanClassName, MAP_NAME)) {
            return true;
        }

        MethodInfo ctor = ctorOpt.get();
        PropertyNamingStrategy namingStrategy = getNamingStrategy(classInfo);

        if (ctor.parametersCount() > 0) {
            for (MethodParameterInfo paramInfo : ctor.parameters()) {
                FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(classInfo, paramInfo, namingStrategy);
                registerTypeToBeGenerated(fieldSpecs.fieldType, fieldSpecs.fieldType.name().toString(), typeBindings);
            }
        }

        Set<String> ignoredProperties = discoverIgnoredProperties(classInfo);

        Set<String> processedFields = new HashSet<>();
        Set<String> boundFieldNames = new HashSet<>();

        for (FieldInfo fieldInfo : classFields(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(classInfo, ctor, fieldInfo, namingStrategy);
            if (fieldSpecs != null) {
                boundFieldNames.add(fieldSpecs.fieldName);
            }
            if (fieldSpecs != null && processedFields.add(fieldSpecs.jsonName)) {
                if (!fieldSpecs.isIgnoredField() && !fieldSpecs.isBackReference()
                        && !isFieldTypeIgnored(fieldSpecs) && !ignoredProperties.contains(fieldSpecs.jsonName)) {
                    registerTypeToBeGenerated(fieldSpecs.fieldType, fieldSpecs.fieldType.name().toString(), typeBindings);
                }
            }
        }

        for (MethodInfo methodInfo : classMethods(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo, namingStrategy);
            if (fieldSpecs != null && !boundFieldNames.contains(fieldSpecs.fieldName)
                    && processedFields.add(fieldSpecs.jsonName)) {
                if (!fieldSpecs.isIgnoredField() && !fieldSpecs.isBackReference()
                        && !isFieldTypeIgnored(fieldSpecs) && !ignoredProperties.contains(fieldSpecs.jsonName)) {
                    registerTypeToBeGenerated(fieldSpecs.fieldType, fieldSpecs.fieldType.name().toString(), typeBindings);
                }
            }
        }

        return true;
    }

    @Override
    protected boolean createSerializationMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName,
            Map<String, Type> typeBindings) {
        if (!discoverFieldTypes(classInfo, beanClassName, typeBindings)) {
            return false;
        }

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

        if (canUseStreaming(classInfo, ctor, beanClassName)) {
            return createStreamingDeserializeMethod(classInfo, classCreator, deserialize, ctor);
        }

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

        boolean valid;
        if (isClassFormatShapeArray(classInfo)) {
            valid = deserializeArrayFields(deserData, deserializedHandle);
        } else if (isAssignableTo(beanClassName, MAP_NAME)) {
            valid = deserializeMapEntries(deserData, deserializedHandle);
        } else {
            valid = deserializeObjectFields(deserData, deserializedHandle);
        }
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
        ResultHandle deserCtx = deserialize.getMethodParam(1);
        return deserialize.invokeVirtualMethod(
                ofMethod(DeserializationContext.class, "readTree", JsonNode.class, JsonParser.class),
                deserCtx, jsonParser);
    }

    private ResultHandle createDeserializedObject(DeserializationData deserData) {
        ResultHandle[] params = new ResultHandle[deserData.constructor.parameters().size()];
        int i = 0;
        for (MethodParameterInfo paramInfo : deserData.constructor.parameters()) {
            FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(deserData.classInfo, paramInfo, deserData.namingStrategy);
            deserData.constructorFields.add(fieldSpecs.jsonName);
            for (String alias : fieldSpecs.aliases) {
                deserData.constructorFields.add(alias);
            }

            ResultHandle fieldValue = lookupJsonField(deserData.methodCreator, deserData.jsonNode, fieldSpecs,
                    deserData.strategyHandle);

            if (fieldSpecs.required) {
                BytecodeCreator missingBranch = deserData.methodCreator.ifNull(fieldValue).trueBranch();
                ResultHandle parser = deserData.methodCreator.getMethodParam(0);
                ResultHandle targetClass = missingBranch.loadClass(deserData.classInfo.name().toString());
                ResultHandle message = missingBranch.load(
                        "Missing required creator property '" + fieldSpecs.jsonName + "'");
                ResultHandle exception = missingBranch.invokeStaticMethod(
                        ofMethod(MismatchedInputException.class, "from",
                                MismatchedInputException.class,
                                JsonParser.class, Class.class, String.class),
                        parser, targetClass, message);
                missingBranch.throwException(exception);
            }

            ResultHandle paramValue = readValueFromJson(deserData.classCreator, deserData.methodCreator,
                    deserData.methodCreator.getMethodParam(1), fieldSpecs, deserData.typeParametersIndex, fieldValue);
            if (paramValue == null) {
                // the value of this parameter cannot be deserialized in a reflection-free way (e.g. its
                // type is polymorphic), so give up generating the deserializer for the whole class
                return null;
            }
            params[i++] = paramValue;
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
        preprocessUnwrappedFields(deserData);

        ResultHandle propertiesSet = deserData.methodCreator
                .invokeVirtualMethod(ofMethod(JsonNode.class, "properties", Set.class), deserData.jsonNode);
        ResultHandle fieldsIterator = deserData.methodCreator
                .invokeInterfaceMethod(ofMethod(Set.class, "iterator", Iterator.class), propertiesSet);
        BytecodeCreator loopCreator = deserData.methodCreator.whileLoop(c -> iteratorHasNext(c, fieldsIterator)).block();
        ResultHandle nextField = loopCreator
                .invokeInterfaceMethod(ofMethod(Iterator.class, "next", Object.class), fieldsIterator);
        ResultHandle mapEntry = loopCreator.checkCast(nextField, Map.Entry.class);
        ResultHandle fieldValue = loopCreator.checkCast(loopCreator
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getValue", Object.class), mapEntry), JsonNode.class);

        ResultHandle fieldName = translateFieldName(deserData, loopCreator, mapEntry);
        Switch.StringSwitch strSwitch = loopCreator.stringSwitch(fieldName);

        // save constructor field names before deserializeFields modifies the set
        // Avoid using Set.copyOf() here as it creates SetN<E> with unstable iteration order.
        Set<String> ctorFields = Collections.unmodifiableSet(new HashSet<>(deserData.constructorFields));

        Set<String> ignoredProperties = new HashSet<>(getIgnoredProperties(deserData.classInfo));
        MethodInfo anyGetterMethod = findAnyGetterMethod(deserData.classInfo);
        if (anyGetterMethod != null) {
            ignoredProperties.add(anyGetterBackingFieldName(anyGetterMethod));
        } else {
            FieldInfo anyGetterField = findAnyGetterField(deserData.classInfo);
            if (anyGetterField != null) {
                ignoredProperties.add(anyGetterField.name());
            }
        }
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
        FieldInfo anySetterField = anySetterMethod == null ? findAnySetterField(deserData.classInfo) : null;
        handleUnknownFields(deserData, ignoredProperties, ctorFields, strSwitch, deserializationContext, fieldName,
                fieldValue, objHandle, anySetterMethod, anySetterField);
        return result;
    }

    private boolean deserializeArrayFields(DeserializationData deserData, ResultHandle objHandle) {
        List<FieldSpecs> orderedFields = collectOrderedFieldSpecs(deserData);
        ResultHandle deserializationContext = deserData.methodCreator.getMethodParam(1);
        boolean valid = true;

        for (int i = 0; i < orderedFields.size(); i++) {
            FieldSpecs fieldSpecs = orderedFields.get(i);
            ResultHandle valueNode = deserData.methodCreator.invokeVirtualMethod(
                    ofMethod(JsonNode.class, "get", JsonNode.class, int.class),
                    deserData.jsonNode, deserData.methodCreator.load(i));
            if (!deserializeField(deserData, deserData.methodCreator, objHandle,
                    valueNode, fieldSpecs, deserializationContext)) {
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Generates deserialization code for classes that extend {@code Map}. Instead of routing JSON fields
     * through a per-property string switch, every key/value pair is put directly into the map instance.
     * This matches standard Jackson's behavior where a {@code Map} subclass is treated as a map, not a bean.
     *
     * <p>
     * For example, for a class extending {@code HashMap<String, Object>}, this generates:
     *
     * <pre>{@code
     * Iterator fields = jsonNode.properties().iterator();
     * while (fields.hasNext()) {
     *     Map.Entry entry = (Map.Entry) fields.next();
     *     JsonNode value = (JsonNode) entry.getValue();
     *     String key = (String) entry.getKey();
     *     bean.put(key, context.readTreeAsValue(value, Object.class));
     * }
     * }</pre>
     */
    private boolean deserializeMapEntries(DeserializationData deserData, ResultHandle objHandle) {
        ResultHandle deserializationContext = deserData.methodCreator().getMethodParam(1);

        ResultHandle propertiesSet = deserData.methodCreator()
                .invokeVirtualMethod(ofMethod(JsonNode.class, "properties", Set.class), deserData.jsonNode());
        ResultHandle fieldsIterator = deserData.methodCreator()
                .invokeInterfaceMethod(ofMethod(Set.class, "iterator", Iterator.class), propertiesSet);
        BytecodeCreator loopCreator = deserData.methodCreator().whileLoop(c -> iteratorHasNext(c, fieldsIterator)).block();
        ResultHandle nextField = loopCreator
                .invokeInterfaceMethod(ofMethod(Iterator.class, "next", Object.class), fieldsIterator);
        ResultHandle mapEntry = loopCreator.checkCast(nextField, Map.Entry.class);
        ResultHandle fieldValue = loopCreator.checkCast(loopCreator
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getValue", Object.class), mapEntry), JsonNode.class);
        ResultHandle fieldName = loopCreator
                .invokeInterfaceMethod(ofMethod(Map.Entry.class, "getKey", Object.class), mapEntry);

        ResultHandle deserializedValue = loopCreator.invokeVirtualMethod(
                ofMethod(DeserializationContext.class, "readTreeAsValue", Object.class, JsonNode.class, Class.class),
                deserializationContext, fieldValue, loopCreator.loadClass(Object.class));

        ResultHandle castedFieldName = loopCreator.checkCast(fieldName, String.class);
        loopCreator.invokeInterfaceMethod(
                ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                objHandle, castedFieldName, deserializedValue);

        return true;
    }

    private List<FieldSpecs> collectOrderedFieldSpecs(DeserializationData deserData) {
        List<FieldSpecs> allSpecs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> boundFieldNames = new HashSet<>();

        for (FieldInfo fieldInfo : classFields(deserData.classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(deserData.classInfo, deserData.constructor, fieldInfo,
                    deserData.namingStrategy);
            if (fieldSpecs != null && seen.add(fieldSpecs.jsonName)
                    && !fieldSpecs.isIgnoredField() && !fieldSpecs.isBackReference() && !isFieldTypeIgnored(fieldSpecs)) {
                allSpecs.add(fieldSpecs);
                boundFieldNames.add(fieldSpecs.fieldName);
            }
        }

        for (MethodInfo methodInfo : classMethods(deserData.classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo, deserData.namingStrategy);
            if (fieldSpecs != null && !boundFieldNames.contains(fieldSpecs.fieldName)
                    && seen.add(fieldSpecs.jsonName)
                    && !fieldSpecs.isIgnoredField() && !isFieldTypeIgnored(fieldSpecs)) {
                allSpecs.add(fieldSpecs);
            }
        }

        String[] propertyOrder = getPropertyOrder(deserData.classInfo);
        if (propertyOrder != null) {
            List<String> orderList = Arrays.asList(propertyOrder);
            allSpecs.sort((a, b) -> {
                int idxA = orderList.indexOf(a.jsonName);
                int idxB = orderList.indexOf(b.jsonName);
                if (idxA == -1 && idxB == -1) {
                    return 0;
                }
                if (idxA == -1) {
                    return 1;
                }
                if (idxB == -1) {
                    return -1;
                }
                return Integer.compare(idxA, idxB);
            });
        }
        return allSpecs;
    }

    private void preprocessUnwrappedFields(DeserializationData deserData) {
        for (FieldInfo fieldInfo : classFields(deserData.classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(deserData.classInfo, deserData.constructor, fieldInfo,
                    deserData.namingStrategy);
            if (fieldSpecs != null && fieldSpecs.isUnwrapped()) {
                String prefix = fieldSpecs.unwrappedPrefix();
                String suffix = fieldSpecs.unwrappedSuffix();
                if (!prefix.isEmpty() || !suffix.isEmpty()) {
                    deserData.methodCreator.invokeStaticMethod(
                            ofMethod(JacksonMapperUtil.class, "collectUnwrappedFields", void.class,
                                    JsonNode.class, String.class, String.class, String.class),
                            deserData.jsonNode,
                            deserData.methodCreator.load(fieldSpecs.jsonName),
                            deserData.methodCreator.load(prefix),
                            deserData.methodCreator.load(suffix));
                }
            }
        }
    }

    private void handleUnknownFields(DeserializationData deserData, Set<String> ignoredProperties,
            Set<String> ctorFields, Switch.StringSwitch strSwitch, ResultHandle deserializationContext,
            ResultHandle fieldName, ResultHandle fieldValue, ResultHandle objHandle, MethodInfo anySetterMethod,
            FieldInfo anySetterField) {
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
        } else if (anySetterField != null) {
            ClassInfo classInfo = deserData.classInfo;
            strSwitch.defaultCase(bytecode -> {
                ResultHandle deserializedValue = bytecode.invokeVirtualMethod(
                        ofMethod(DeserializationContext.class, "readTreeAsValue", Object.class, JsonNode.class, Class.class),
                        deserializationContext, fieldValue,
                        bytecode.loadClass(Object.class));
                ResultHandle castedFieldName = bytecode.checkCast(fieldName, String.class);
                MethodInfo getter = findMethod(classInfo, "get" + ucFirst(anySetterField.name()));
                ResultHandle map;
                if (getter != null) {
                    map = bytecode.invokeVirtualMethod(MethodDescriptor.of(getter), objHandle);
                } else {
                    map = bytecode.readInstanceField(FieldDescriptor.of(anySetterField), objHandle);
                }
                bytecode.invokeInterfaceMethod(
                        ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                        map, castedFieldName, deserializedValue);
            });
        } else if (shouldIgnoreUnknownProperties(deserData.classInfo)) {
            strSwitch.defaultCase(bytecode -> {
            });
        } else {
            strSwitch.defaultCase(bytecode -> {
                ResultHandle failOnUnknown = bytecode.invokeVirtualMethod(
                        ofMethod(tools.jackson.databind.DeserializationContext.class, "isEnabled", boolean.class,
                                DeserializationFeature.class),
                        deserializationContext,
                        bytecode.readStaticField(FieldDescriptor.of(DeserializationFeature.class,
                                "FAIL_ON_UNKNOWN_PROPERTIES", DeserializationFeature.class)));
                BytecodeCreator trueBranch = bytecode.ifTrue(failOnUnknown).trueBranch();
                ResultHandle parser = deserData.methodCreator.getMethodParam(0);
                ResultHandle targetClass = trueBranch.loadClass(deserData.classInfo.name().toString());
                ResultHandle castedFieldName = trueBranch.checkCast(fieldName, String.class);
                ResultHandle exception = trueBranch.invokeStaticMethod(
                        ofMethod(UnrecognizedPropertyException.class, "from", UnrecognizedPropertyException.class,
                                JsonParser.class, Object.class, String.class, Collection.class),
                        parser, targetClass, castedFieldName, trueBranch.loadNull());
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
                FieldSpecs fieldSpecs = fieldSpecsFromFieldParam(classInfo, paramInfo, namingStrategy);
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
                .getMethodCreator("createContextual", ValueDeserializer.class, DeserializationContext.class, BeanProperty.class)
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
        Set<String> boundFieldNames = new HashSet<>();

        for (FieldInfo fieldInfo : classFields(deserData.classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(deserData.classInfo, deserData.constructor, fieldInfo,
                    deserData.namingStrategy);
            if (fieldSpecs != null) {
                boundFieldNames.add(fieldSpecs.fieldName);
            }
            deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch, fieldSpecs, valid);
        }

        for (MethodInfo methodInfo : classMethods(deserData.classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo, deserData.namingStrategy);
            if (fieldSpecs != null && boundFieldNames.contains(fieldSpecs.fieldName)) {
                continue;
            }
            deserializeFieldSpecs(deserData, deserializationContext, objHandle, fieldValue,
                    deserializedFields, strSwitch, fieldSpecs, valid);
        }

        return valid.get();
    }

    private void deserializeFieldSpecs(DeserializationData deserData, ResultHandle deserializationContext,
            ResultHandle objHandle, ResultHandle fieldValue, Set<String> deserializedFields, Switch.StringSwitch strSwitch,
            FieldSpecs fieldSpecs, AtomicBoolean valid) {
        if (fieldSpecs != null && deserializedFields.add(fieldSpecs.jsonName)) {
            if (fieldSpecs.isIgnoredField() || fieldSpecs.isBackReference() || isFieldTypeIgnored(fieldSpecs)) {
                return;
            }
            strSwitch.caseOf(fieldSpecs.jsonName,
                    bytecode -> valid.compareAndSet(true, deserializeField(deserData, bytecode, objHandle,
                            fieldValue, fieldSpecs, deserializationContext)));
            for (String alias : fieldSpecs.aliases) {
                if (alias.equals(fieldSpecs.jsonName)) {
                    continue;
                }
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
                    ofMethod(JacksonException.class, "prependPath", JacksonException.class, Object.class, String.class),
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

    private FieldInfo findAnySetterField(ClassInfo classInfo) {
        for (FieldInfo field : classFields(classInfo)) {
            if (field.hasAnnotation(JsonAnySetter.class)
                    && !Modifier.isStatic(field.flags())) {
                return field;
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
                && (methodInfo.name().startsWith("set") || methodInfo.hasAnnotation(JsonSetter.class));
    }

    private ResultHandle readValueFromJson(ClassCreator classCreator, BytecodeCreator bytecode,
            ResultHandle deserializationContext, FieldSpecs fieldSpecs, Map<String, Integer> typeParametersIndex,
            ResultHandle valueNode) {
        Type fieldType = fieldSpecs.fieldType;
        String fieldTypeName = fieldType.name().toString();
        if (JacksonSerializationUtils.isBasicJsonType(fieldType)) {
            return readValueForPrimitiveFields(bytecode, fieldType, valueNode);
        }

        if (hasJsonTypeInfoInTypeChain(fieldType)) {
            return null;
        }

        FieldKind fieldKind = classifyFieldType(fieldType, fieldTypeName);
        ResultHandle typeHandle = switch (fieldKind) {
            case TYPE_VARIABLE -> readTypeVariable(classCreator, bytecode, fieldType.asTypeVariable(), typeParametersIndex);
            case LIST, SET, WRAPPER, MAP -> {
                MethodDescriptor getTypeFactory = ofMethod(DeserializationContext.class, "getTypeFactory",
                        TypeFactory.class);
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(getTypeFactory, deserializationContext);
                yield buildJavaType(classCreator, bytecode, typeFactory, fieldType, typeParametersIndex);
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

    /**
     * Reads the {@code JavaType} bound to a type variable from the {@code valueTypes} field that
     * {@code createContextual} populates at runtime. Returns {@code null} when the variable is not
     * a type parameter of the deserialized class.
     */
    private static ResultHandle readTypeVariable(ClassCreator classCreator, BytecodeCreator bytecode,
            TypeVariable typeVariable, Map<String, Integer> typeParametersIndex) {
        Integer parameterIndex = typeParametersIndex.get(typeVariable.identifier());
        if (parameterIndex == null) {
            return null;
        }
        FieldDescriptor valueTypesField = FieldDescriptor.of(classCreator.getClassName(), "valueTypes", JavaType[].class);
        ResultHandle valueTypes = bytecode.readInstanceField(valueTypesField, bytecode.getThis());
        return bytecode.readArrayValue(valueTypes, parameterIndex);
    }

    /**
     * Emits bytecode that constructs the Jackson {@code JavaType} for the given type, recursing into
     * type arguments so a nested type, like the {@code List<Foo>} in a {@code Map<String, List<Foo>>},
     * keeps its generics instead of collapsing to its raw class (which would deserialize its elements
     * as {@code LinkedHashMap}s).
     *
     * Returns {@code null} when the type or one of its arguments cannot be resolved at build time; the
     * caller then abandons the generated deserializer and Jackson falls back to reflection.
     */
    private static ResultHandle buildJavaType(ClassCreator classCreator, BytecodeCreator bytecode,
            ResultHandle typeFactory, Type type, Map<String, Integer> typeParametersIndex) {
        switch (type.kind()) {
            case CLASS:
                return bytecode.invokeVirtualMethod(
                        ofMethod(TypeFactory.class, "constructType", JavaType.class, java.lang.reflect.Type.class),
                        typeFactory, bytecode.loadClass(type.name().toString()));
            case TYPE_VARIABLE:
                return readTypeVariable(classCreator, bytecode, type.asTypeVariable(), typeParametersIndex);
            case WILDCARD_TYPE:
                // resolve a wildcard to its upper bound, which Jandex defaults to Object
                return buildJavaType(classCreator, bytecode, typeFactory, type.asWildcardType().extendsBound(),
                        typeParametersIndex);
            case PARAMETERIZED_TYPE:
                List<Type> arguments = type.asParameterizedType().arguments();
                ResultHandle argumentTypes = bytecode.newArray(JavaType.class, arguments.size());
                for (int i = 0; i < arguments.size(); i++) {
                    ResultHandle argumentType = buildJavaType(classCreator, bytecode, typeFactory, arguments.get(i),
                            typeParametersIndex);
                    if (argumentType == null) {
                        return null;
                    }
                    bytecode.writeArrayValue(argumentTypes, i, argumentType);
                }
                return bytecode.invokeVirtualMethod(
                        ofMethod(TypeFactory.class, "constructParametricType", JavaType.class, Class.class, JavaType[].class),
                        typeFactory, bytecode.loadClass(type.name().toString()), argumentTypes);
            default:
                // arrays and primitives nested in a generic type are not supported yet
                return null;
        }
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
    protected Optional<MethodInfo> findConstructor(ClassInfo classInfo) {
        Optional<MethodInfo> ctorOpt = super.findConstructor(classInfo);
        if (ctorOpt.isPresent() && ctorOpt.get().parametersCount() == 0 && !classInfo.isRecord()) {
            Set<String> unsettableFields = findUnsettableFields(classInfo);
            if (!unsettableFields.isEmpty()) {
                return classInfo.constructors().stream()
                        .filter(ctor -> Modifier.isPublic(ctor.flags()) && ctorCoversFields(ctor, unsettableFields))
                        .findFirst();
            }
        }
        return ctorOpt;
    }

    private static boolean ctorCoversFields(MethodInfo ctor, Set<String> fieldNames) {
        Set<String> paramNames = new HashSet<>();
        for (MethodParameterInfo param : ctor.parameters()) {
            paramNames.add(param.name());
        }
        return paramNames.containsAll(fieldNames);
    }

    private Set<String> findUnsettableFields(ClassInfo classInfo) {
        Set<String> unsettable = new HashSet<>();
        for (FieldInfo field : classFields(classInfo)) {
            if (Modifier.isStatic(field.flags()) || Modifier.isPublic(field.flags())) {
                continue;
            }
            if (getterMethodInfo(classInfo, field) == null) {
                continue;
            }
            if (findMethod(classInfo, "set" + ucFirst(field.name()), field.type()) != null
                    || findMethod(classInfo, field.name(), field.type()) != null) {
                continue;
            }
            String typeName = field.type().name().toString();
            if (isAssignableTo(typeName, COLLECTION_NAME) || isAssignableTo(typeName, MAP_NAME)) {
                continue;
            }
            unsettable.add(field.name());
        }
        return unsettable;
    }

    @Override
    protected boolean shouldGenerateCodeFor(ClassInfo classInfo) {
        return super.shouldGenerateCodeFor(classInfo) && classInfo.hasNoArgsConstructor();
    }

    // ── Streaming deserialization ──────────────────────────────────────────────

    private static final String FIELD_MATCHER_NAME = "FIELD_MATCHER";

    private record MatcherEntry(String name, FieldSpecs fieldSpecs) {
    }

    private boolean createStreamingDeserializeMethod(ClassInfo classInfo, ClassCreator classCreator,
            MethodCreator deserialize, MethodInfo ctor) {

        PropertyNamingStrategy namingStrategy = getNamingStrategy(classInfo);
        Set<String> translatableNames = collectTranslatableFieldNames(classInfo, ctor, namingStrategy);
        List<MatcherEntry> entries = collectMatcherEntries(classInfo, ctor, namingStrategy);

        boolean hasTranslatableNames = !translatableNames.isEmpty();
        generateStreamingStaticInit(classCreator, entries, translatableNames);

        ResultHandle strategyHandle = hasTranslatableNames ? getStrategyHandle(deserialize) : null;

        ResultHandle activeViewHandle = deserialize.invokeVirtualMethod(
                ofMethod(DeserializationContext.class, "getActiveView", Class.class),
                deserialize.getMethodParam(1));

        Map<String, Integer> typeParametersIndex = parseTypeParameters(classInfo, classCreator);

        ResultHandle objHandle = deserialize.newInstance(
                MethodDescriptor.ofConstructor(classInfo.name().toString()));

        ResultHandle parser = deserialize.getMethodParam(0);
        ResultHandle ctxt = deserialize.getMethodParam(1);

        AssignableResultHandle nameVar = deserialize.createVariable(String.class);
        BytecodeCreator loopBody = deserialize.whileLoop(c -> {
            ResultHandle name = c.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "nextName", String.class), parser);
            c.assign(nameVar, name);
            return c.ifNotNull(name);
        }).block();

        loopBody.invokeVirtualMethod(
                ofMethod(JsonParser.class, "nextToken", JsonToken.class), parser);

        AtomicBoolean valid = new AtomicBoolean(true);
        generateStreamingFieldDispatch(classInfo, classCreator, loopBody, parser, ctxt, objHandle,
                entries, typeParametersIndex, activeViewHandle, strategyHandle,
                hasTranslatableNames, nameVar, valid);

        deserialize.returnValue(objHandle);
        return valid.get();
    }

    private List<MatcherEntry> collectMatcherEntries(ClassInfo classInfo, MethodInfo ctor,
            PropertyNamingStrategy namingStrategy) {
        List<MatcherEntry> entries = new ArrayList<>();
        Set<String> addedNames = new HashSet<>();
        Set<String> boundFieldNames = new HashSet<>();

        for (FieldInfo fieldInfo : classFields(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromField(classInfo, ctor, fieldInfo, namingStrategy);
            if (fieldSpecs != null) {
                boundFieldNames.add(fieldSpecs.fieldName);
            }
            if (fieldSpecs != null && addedNames.add(fieldSpecs.jsonName)
                    && !fieldSpecs.isIgnoredField() && !fieldSpecs.isBackReference()
                    && !isFieldTypeIgnored(fieldSpecs)) {
                entries.add(new MatcherEntry(fieldSpecs.jsonName, fieldSpecs));
                for (String alias : fieldSpecs.aliases) {
                    if (!alias.equals(fieldSpecs.jsonName) && addedNames.add(alias)) {
                        entries.add(new MatcherEntry(alias, fieldSpecs));
                    }
                }
            }
        }

        for (MethodInfo methodInfo : classMethods(classInfo)) {
            FieldSpecs fieldSpecs = fieldSpecsFromMethod(methodInfo, namingStrategy);
            if (fieldSpecs != null && !boundFieldNames.contains(fieldSpecs.fieldName)
                    && addedNames.add(fieldSpecs.jsonName)
                    && !fieldSpecs.isIgnoredField() && !isFieldTypeIgnored(fieldSpecs)) {
                entries.add(new MatcherEntry(fieldSpecs.jsonName, fieldSpecs));
                for (String alias : fieldSpecs.aliases) {
                    if (!alias.equals(fieldSpecs.jsonName) && addedNames.add(alias)) {
                        entries.add(new MatcherEntry(alias, fieldSpecs));
                    }
                }
            }
        }

        for (String ignored : discoverIgnoredProperties(classInfo)) {
            if (addedNames.add(ignored)) {
                entries.add(new MatcherEntry(ignored, null));
            }
        }

        return entries;
    }

    private void generateStreamingStaticInit(ClassCreator classCreator,
            List<MatcherEntry> entries, Set<String> translatableNames) {
        MethodCreator clinit = classCreator.getMethodCreator("<clinit>", void.class)
                .setModifiers(ACC_STATIC);

        if (!translatableNames.isEmpty()) {
            ResultHandle namesArray = clinit.newArray(String.class, translatableNames.size());
            int i = 0;
            for (String name : translatableNames) {
                clinit.writeArrayValue(namesArray, i++, clinit.load(name));
            }
            FieldCreator namesField = classCreator
                    .getFieldCreator(TRANSLATABLE_FIELD_NAMES, String[].class.getName())
                    .setModifiers(ACC_STATIC | ACC_FINAL);
            clinit.writeStaticField(namesField.getFieldDescriptor(), namesArray);
        }

        ResultHandle namesList = clinit.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (MatcherEntry entry : entries) {
            clinit.invokeInterfaceMethod(
                    ofMethod(List.class, "add", boolean.class, Object.class),
                    namesList, clinit.load(entry.name()));
        }
        ResultHandle locale = clinit.invokeStaticMethod(
                ofMethod(Locale.class, "getDefault", Locale.class));
        ResultHandle matcher = clinit.invokeStaticMethod(
                ofMethod(SimpleNameMatcher.class, "construct", SimpleNameMatcher.class,
                        Locale.class, List.class),
                locale, namesList);
        FieldCreator matcherField = classCreator
                .getFieldCreator(FIELD_MATCHER_NAME, PropertyNameMatcher.class.getName())
                .setModifiers(ACC_STATIC | ACC_FINAL);
        clinit.writeStaticField(matcherField.getFieldDescriptor(), matcher);

        clinit.returnVoid();
    }

    private void generateStreamingFieldDispatch(ClassInfo classInfo, ClassCreator classCreator,
            BytecodeCreator loopBody, ResultHandle parser, ResultHandle ctxt, ResultHandle objHandle,
            List<MatcherEntry> entries, Map<String, Integer> typeParametersIndex,
            ResultHandle activeViewHandle, ResultHandle strategyHandle,
            boolean hasTranslatableNames, ResultHandle nameVar, AtomicBoolean valid) {

        ResultHandle matcherHandle = loopBody.readStaticField(
                FieldDescriptor.of(classCreator.getClassName(), FIELD_MATCHER_NAME,
                        PropertyNameMatcher.class));
        ResultHandle strategyArg = hasTranslatableNames ? strategyHandle : loopBody.loadNull();
        ResultHandle namesArg = hasTranslatableNames
                ? loopBody.readStaticField(
                        FieldDescriptor.of(classCreator.getClassName(), TRANSLATABLE_FIELD_NAMES, String[].class))
                : loopBody.loadNull();
        ResultHandle ix = loopBody.invokeStaticMethod(
                ofMethod(JacksonMapperUtil.class, "matchFieldName", int.class,
                        PropertyNameMatcher.class, String.class, PropertyNamingStrategy.class, String[].class),
                matcherHandle, nameVar, strategyArg, namesArg);

        BytecodeCreator dispatch = loopBody.createScope();
        for (int i = 0; i < entries.size(); i++) {
            MatcherEntry entry = entries.get(i);
            BranchResult cmp = dispatch.ifIntegerEqual(ix, dispatch.load(i));
            BytecodeCreator matchBranch = cmp.trueBranch();

            if (entry.fieldSpecs() != null) {
                valid.compareAndSet(true, deserializeFieldStreaming(classInfo, classCreator,
                        matchBranch, parser, ctxt, objHandle, entry.fieldSpecs(),
                        typeParametersIndex, activeViewHandle));
            } else {
                matchBranch.invokeVirtualMethod(
                        ofMethod(JsonParser.class, "skipChildren", JsonParser.class), parser);
            }

            matchBranch.breakScope(dispatch);
        }

        generateStreamingUnknownFieldHandler(classInfo, dispatch, parser, ctxt, objHandle);
    }

    private boolean deserializeFieldStreaming(ClassInfo classInfo, ClassCreator classCreator,
            BytecodeCreator bytecode, ResultHandle parser, ResultHandle ctxt,
            ResultHandle objHandle, FieldSpecs fieldSpecs,
            Map<String, Integer> typeParametersIndex, ResultHandle activeViewHandle) {

        String[] viewClasses = fieldSpecs.viewClasses();
        if (viewClasses != null) {
            ResultHandle viewClassesArray = bytecode.newArray(Class.class, viewClasses.length);
            for (int i = 0; i < viewClasses.length; i++) {
                bytecode.writeArrayValue(viewClassesArray, i, bytecode.loadClass(viewClasses[i]));
            }
            ResultHandle included = bytecode.invokeStaticMethod(
                    ofMethod(JacksonMapperUtil.class, "isViewIncluded", boolean.class, Class.class, Class[].class),
                    activeViewHandle, viewClassesArray);
            BranchResult viewBranch = bytecode.ifTrue(included);
            viewBranch.falseBranch().invokeVirtualMethod(
                    ofMethod(JsonParser.class, "skipChildren", JsonParser.class), parser);
            bytecode = viewBranch.trueBranch();
        }

        boolean isBasicType = JacksonSerializationUtils.isBasicJsonType(fieldSpecs.fieldType);

        BytecodeCreator effectiveBytecode = bytecode;
        TryBlock tryBlock = null;
        if (!isBasicType) {
            tryBlock = bytecode.tryBlock();
            effectiveBytecode = tryBlock;
        }

        ResultHandle valueHandle = readValueFromJsonStreaming(classCreator, effectiveBytecode, parser, ctxt,
                fieldSpecs, typeParametersIndex);
        if (valueHandle == null) {
            return false;
        }

        writeValueToObject(classInfo, objHandle, fieldSpecs, effectiveBytecode,
                fieldSpecs.toValueWriterHandle(effectiveBytecode, valueHandle));

        if (tryBlock != null) {
            CatchBlockCreator catchBlock = tryBlock.addCatch(MismatchedInputException.class);
            ResultHandle exception = catchBlock.getCaughtException();
            catchBlock.invokeVirtualMethod(
                    ofMethod(JacksonException.class, "prependPath", JacksonException.class, Object.class, String.class),
                    exception, objHandle, catchBlock.load(fieldSpecs.jsonName));
            catchBlock.throwException(exception);
        }

        return true;
    }

    private ResultHandle readValueFromJsonStreaming(ClassCreator classCreator, BytecodeCreator bytecode,
            ResultHandle parser, ResultHandle deserializationContext,
            FieldSpecs fieldSpecs, Map<String, Integer> typeParametersIndex) {
        Type fieldType = fieldSpecs.fieldType;
        String fieldTypeName = fieldType.name().toString();

        if (JacksonSerializationUtils.isBasicJsonType(fieldType)) {
            return readPrimitiveFromParser(bytecode, fieldType, parser);
        }

        if (hasJsonTypeInfoInTypeChain(fieldType)) {
            return null;
        }

        FieldKind fieldKind = classifyFieldType(fieldType, fieldTypeName);
        ResultHandle typeHandle = switch (fieldKind) {
            case TYPE_VARIABLE -> readTypeVariable(classCreator, bytecode, fieldType.asTypeVariable(), typeParametersIndex);
            case LIST, SET, WRAPPER, MAP -> {
                ResultHandle typeFactory = bytecode.invokeVirtualMethod(
                        ofMethod(DeserializationContext.class, "getTypeFactory", TypeFactory.class),
                        deserializationContext);
                yield buildJavaType(classCreator, bytecode, typeFactory, fieldType, typeParametersIndex);
            }
            default -> bytecode.loadClass(fieldTypeName);
        };

        if (typeHandle == null) {
            return null;
        }

        MethodDescriptor readValue = ofMethod(DeserializationContext.class, "readValue",
                Object.class, JsonParser.class, fieldKind.isGeneric() ? JavaType.class : Class.class);
        return bytecode.invokeVirtualMethod(readValue, deserializationContext, parser, typeHandle);
    }

    private static ResultHandle readPrimitiveFromParser(BytecodeCreator bytecode, Type fieldType,
            ResultHandle parser) {
        AssignableResultHandle result = bytecode.createVariable(DescriptorUtils.typeToString(fieldType));

        ResultHandle currentToken = bytecode.invokeVirtualMethod(
                ofMethod(JsonParser.class, "currentToken", JsonToken.class), parser);
        ResultHandle nullToken = bytecode.readStaticField(
                FieldDescriptor.of(JsonToken.class, "VALUE_NULL", JsonToken.class));
        BranchResult isNull = bytecode.ifReferencesEqual(currentToken, nullToken);

        isNull.trueBranch().assign(result, JacksonSerializationUtils.getDefaultValue(isNull.trueBranch(), fieldType));

        BytecodeCreator notNull = isNull.falseBranch();

        ResultHandle convertedValue = switch (fieldType.name().toString()) {
            case "java.lang.String" -> notNull.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "getText", String.class), parser);
            case "char", "java.lang.Character" -> notNull.invokeVirtualMethod(
                    ofMethod(String.class, "charAt", char.class, int.class),
                    notNull.invokeVirtualMethod(ofMethod(JsonParser.class, "getText", String.class), parser),
                    notNull.load(0));
            case "short", "java.lang.Short" -> notNull.convertPrimitive(
                    notNull.invokeVirtualMethod(ofMethod(JsonParser.class, "getValueAsInt", int.class), parser),
                    short.class);
            case "int" -> notNull.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "getValueAsInt", int.class), parser);
            case "java.lang.Integer" -> notNull.invokeStaticMethod(
                    ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                    notNull.invokeVirtualMethod(ofMethod(JsonParser.class, "getValueAsInt", int.class), parser));
            case "long", "java.lang.Long" -> notNull.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "getValueAsLong", long.class), parser);
            case "float", "java.lang.Float" -> notNull.convertPrimitive(
                    notNull.invokeVirtualMethod(ofMethod(JsonParser.class, "getValueAsDouble", double.class), parser),
                    float.class);
            case "double", "java.lang.Double" -> notNull.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "getValueAsDouble", double.class), parser);
            case "boolean", "java.lang.Boolean" -> notNull.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "getValueAsBoolean", boolean.class), parser);
            default -> throw new IllegalStateException("Type " + fieldType + " should be handled by the switch");
        };

        notNull.assign(result, convertedValue);
        return result;
    }

    private void generateStreamingUnknownFieldHandler(ClassInfo classInfo, BytecodeCreator bytecode,
            ResultHandle parser, ResultHandle ctxt, ResultHandle objHandle) {
        MethodInfo anySetterMethod = findAnySetterMethod(classInfo);
        FieldInfo anySetterField = anySetterMethod == null ? findAnySetterField(classInfo) : null;

        if (anySetterMethod != null) {
            ResultHandle name = bytecode.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "currentName", String.class), parser);
            ResultHandle value = bytecode.invokeVirtualMethod(
                    ofMethod(DeserializationContext.class, "readValue", Object.class, JsonParser.class, Class.class),
                    ctxt, parser, bytecode.loadClass(anySetterMethod.parameterType(1).name().toString()));
            if (anySetterMethod.declaringClass().isInterface()) {
                bytecode.invokeInterfaceMethod(anySetterMethod, objHandle, name, value);
            } else {
                bytecode.invokeVirtualMethod(anySetterMethod, objHandle, name, value);
            }
        } else if (anySetterField != null) {
            ResultHandle name = bytecode.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "currentName", String.class), parser);
            ResultHandle value = bytecode.invokeVirtualMethod(
                    ofMethod(DeserializationContext.class, "readValue", Object.class, JsonParser.class, Class.class),
                    ctxt, parser, bytecode.loadClass(Object.class));
            MethodInfo getter = findMethod(classInfo, "get" + ucFirst(anySetterField.name()));
            ResultHandle map;
            if (getter != null) {
                map = bytecode.invokeVirtualMethod(MethodDescriptor.of(getter), objHandle);
            } else {
                map = bytecode.readInstanceField(FieldDescriptor.of(anySetterField), objHandle);
            }
            bytecode.invokeInterfaceMethod(
                    ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                    map, name, value);
        } else if (shouldIgnoreUnknownProperties(classInfo)) {
            bytecode.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "skipChildren", JsonParser.class), parser);
        } else {
            ResultHandle failOnUnknown = bytecode.invokeVirtualMethod(
                    ofMethod(DeserializationContext.class, "isEnabled", boolean.class, DeserializationFeature.class),
                    ctxt,
                    bytecode.readStaticField(FieldDescriptor.of(DeserializationFeature.class,
                            "FAIL_ON_UNKNOWN_PROPERTIES", DeserializationFeature.class)));
            BytecodeCreator trueBranch = bytecode.ifTrue(failOnUnknown).trueBranch();
            ResultHandle targetClass = trueBranch.loadClass(classInfo.name().toString());
            ResultHandle unknownName = trueBranch.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "currentName", String.class), parser);
            ResultHandle exception = trueBranch.invokeStaticMethod(
                    ofMethod(UnrecognizedPropertyException.class, "from", UnrecognizedPropertyException.class,
                            JsonParser.class, Object.class, String.class, Collection.class),
                    parser, targetClass, unknownName, trueBranch.loadNull());
            trueBranch.throwException(exception);

            bytecode.invokeVirtualMethod(
                    ofMethod(JsonParser.class, "skipChildren", JsonParser.class), parser);
        }
    }

    // ── Tree-based deserialization (fallback) ───────────────────────────────────

    private static final String TRANSLATABLE_FIELD_NAMES = "TRANSLATABLE_FIELD_NAMES";

    private record DeserializationData(ClassInfo classInfo, MethodInfo constructor, ClassCreator classCreator,
            MethodCreator methodCreator,
            ResultHandle jsonNode, Map<String, Integer> typeParametersIndex, Set<String> constructorFields,
            PropertyNamingStrategy namingStrategy, ResultHandle strategyHandle, ResultHandle reverseIndexHandle,
            ResultHandle activeViewHandle) {
    }
}
