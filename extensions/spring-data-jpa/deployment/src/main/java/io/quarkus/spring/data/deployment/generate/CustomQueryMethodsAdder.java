package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.spring.data.deployment.generate.GenerationUtil.getNamedQueryForMethod;
import static java.util.function.Predicate.not;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.deployment.MethodNameParser;
import io.quarkus.spring.data.runtime.TypesConverter;

public class CustomQueryMethodsAdder extends AbstractMethodsAdder {

    private static final String QUERY_VALUE_FIELD = "value";
    private static final String QUERY_COUNT_FIELD = "countQuery";
    private static final String NAMED_QUERY_FIELD = "query";

    private static final Pattern SELECT_CLAUSE = Pattern.compile("select\\s+(.+)\\s+from", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_ALIAS = Pattern.compile(".*\\s+[as|AS]+\\s+([\\w\\.]+)");
    private static final Pattern FIELD_NAME = Pattern.compile("(\\w+).*");
    private static final Pattern NAMED_PARAMETER = Pattern.compile("\\:(\\w+)\\b");

    private final IndexView index;
    private final ClassOutput nonBeansClassOutput;
    private final Consumer<String> customClassCreatedCallback;
    private final FieldDesc operationsField;

    public CustomQueryMethodsAdder(IndexView index, ClassOutput classOutput,
            Consumer<String> customClassCreatedCallback,
            TypeBundle typeBundle) {
        this.index = index;
        this.nonBeansClassOutput = classOutput;
        this.customClassCreatedCallback = customClassCreatedCallback;
        String operationsName = typeBundle.operations().dotName().toString();
        operationsField = FieldDesc.of(ClassDesc.of(operationsName), "INSTANCE", ClassDesc.of(operationsName));
    }

    public void add(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor, ClassInfo repositoryClassInfo,
            ClassInfo entityClassInfo, String idTypeStr, Set<String> existingMethods) {

        // Remember custom return types: {resultType:{methodName:[fieldNames]}}
        Map<DotName, Map<String, List<String>>> customResultTypes = new HashMap<>(3);
        Map<DotName, DotName> customResultTypeNames = new HashMap<>(3);
        Set<DotName> entityFieldTypeNames = new HashSet<>();

        for (MethodInfo method : repositoryClassInfo.methods()) {

            AnnotationInstance queryInstance = method.annotation(DotNames.SPRING_DATA_QUERY);
            AnnotationInstance namedQueryInstance = getNamedQueryForMethod(method, entityClassInfo);

            String methodName = method.name();
            String repositoryName = repositoryClassInfo.name().toString();
            String queryString;
            if (queryInstance != null) {
                verifyQueryAnnotation(queryInstance, methodName, repositoryName);
                queryString = queryInstance.value(QUERY_VALUE_FIELD).asString().trim();
            } else if (namedQueryInstance != null) {
                queryString = namedQueryInstance.value(NAMED_QUERY_FIELD).asString().trim();
            } else {
                // handled by DerivedMethodsAdder
                continue;
            }

            if (queryString.contains("#{")) {
                throw new IllegalArgumentException("spEL expressions are not currently supported. " +
                        "Offending method is " + methodName + " of Repository " + repositoryName);
            }

            if (!(queryString.startsWith("select") || queryString.startsWith("SELECT")
                    || queryString.startsWith("from") || queryString.startsWith("FROM")
                    || queryString.startsWith("delete") || queryString.startsWith("DELETE")
                    || queryString.startsWith("update") || queryString.startsWith("UPDATE"))) {
                throw new IllegalArgumentException("Unsupported query type in @Query. " +
                        "Offending method is " + methodName + " of Repository " + repositoryName);
            }

            List<Type> methodParameterTypes = method.parameterTypes();
            String[] methodParameterTypesStr = new String[methodParameterTypes.size()];
            List<Integer> queryParameterIndexes = new ArrayList<>(methodParameterTypes.size());
            Integer pageableParameterIndex = null;
            Integer sortParameterIndex = null;
            for (int i = 0; i < methodParameterTypes.size(); i++) {
                DotName parameterType = methodParameterTypes.get(i).name();
                methodParameterTypesStr[i] = parameterType.toString();
                if (DotNames.SPRING_DATA_PAGEABLE.equals(parameterType)
                        || DotNames.SPRING_DATA_PAGE_REQUEST.equals(parameterType)) {
                    if (pageableParameterIndex != null) {
                        throw new IllegalArgumentException("Method " + method.name() + " of Repository " + repositoryClassInfo
                                + "has invalid parameters - only a single parameter of type" + DotNames.SPRING_DATA_PAGEABLE
                                + " can be specified");
                    }
                    pageableParameterIndex = i;
                } else if (DotNames.SPRING_DATA_SORT.equals(parameterType)) {
                    if (sortParameterIndex != null) {
                        throw new IllegalArgumentException("Method " + method.name() + " of Repository " + repositoryClassInfo
                                + "has invalid parameters - only a single parameter of type" + DotNames.SPRING_DATA_SORT
                                + " can be specified");
                    }
                    sortParameterIndex = i;
                } else {
                    queryParameterIndexes.add(i);
                }
            }

            // go through the method annotations, find the @Param annotation on parameters
            // and map the name to the method param index
            Map<String, Integer> namedParameterToIndex = new HashMap<>();
            for (AnnotationInstance annotation : method.annotations(DotNames.SPRING_DATA_PARAM)) {
                var index = (int) annotation.target().asMethodParameter().position();
                namedParameterToIndex.put(annotation.value().asString(), index);
            }
            // if no or only some parameters are annotated with @Param, add the compiled names (if present)
            if (namedParameterToIndex.size() < methodParameterTypes.size()) {
                for (int index = 0; index < methodParameterTypes.size(); index++) {
                    if (namedParameterToIndex.containsValue(index)) {
                        continue;
                    }
                    String parameterName = method.parameterName(index);
                    if (parameterName != null) {
                        namedParameterToIndex.put(parameterName, index);
                    }
                }
            }

            boolean isModifying = (method.annotation(DotNames.SPRING_DATA_MODIFYING) != null);
            if (isModifying && (sortParameterIndex != null || pageableParameterIndex != null)) {
                throw new IllegalArgumentException(
                        method.name() + " of Repository " + repositoryClassInfo
                                + " is meant to be a insert/update/delete query and therefore doesn't " +
                                "support Pageable and Sort method parameters");
            }

            Set<String> usedNamedParameters = extractNamedParameters(queryString);
            if (!usedNamedParameters.isEmpty()) {
                Set<String> missingParameters = new LinkedHashSet<>(usedNamedParameters);
                missingParameters.removeAll(namedParameterToIndex.keySet());
                if (!missingParameters.isEmpty()) {
                    throw new IllegalArgumentException(
                            method.name() + " of Repository " + repositoryClassInfo
                                    + " is missing the named parameters " + missingParameters
                                    + ", provided are " + namedParameterToIndex.keySet()
                                    + ". Ensure that the parameters are correctly annotated with @Param.");
                }
                namedParameterToIndex.keySet().retainAll(usedNamedParameters);
            } else {
                namedParameterToIndex.clear();
            }

            DotName methodReturnTypeDotName = method.returnType().name();

            // Need effectively final copies for use in lambdas
            final Integer finalPageableParameterIndex = pageableParameterIndex;
            final Integer finalSortParameterIndex = sortParameterIndex;
            final String finalQueryString = queryString;
            final Map<String, Integer> finalNamedParameterToIndex = namedParameterToIndex;

            MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(methodReturnTypeDotName.toString(),
                    methodParameterTypesStr);

            classCreator.method(method.name(), mc -> {
                mc.setType(mtd);

                // Add @Transactional for modifying queries before calling body()
                if (isModifying) {
                    mc.addAnnotation(Transactional.class);
                }

                // Declare parameters
                ParamVar[] params = new ParamVar[methodParameterTypes.size()];
                for (int i = 0; i < methodParameterTypes.size(); i++) {
                    params[i] = mc.parameter("p" + i);
                }

                mc.body(bc -> {
                    // Store static field and instance field in LocalVars so they can be reused
                    LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                    LocalVar entityClass = bc.localVar("entityClass",
                            bc.get(mc.this_().field(entityClassFieldDescriptor)));

                    if (isModifying) {
                        AnnotationInstance modifyingAnnotation = method.annotation(DotNames.SPRING_DATA_MODIFYING);
                        handleFlushAutomatically(modifyingAnnotation, bc, entityClass);

                        if (finalQueryString.toLowerCase().startsWith("delete")) {
                            if (!DotNames.PRIMITIVE_LONG.equals(methodReturnTypeDotName)
                                    && !DotNames.LONG.equals(methodReturnTypeDotName)
                                    && !DotNames.VOID.equals(methodReturnTypeDotName)) {
                                throw new IllegalArgumentException(
                                        method.name() + " of Repository " + repositoryClassInfo
                                                + " is meant to be a delete query and can therefore only have a void or long return type");
                            }

                            // we need to strip 'delete' or else JpaOperations.delete will generate the wrong query
                            String deleteQueryString = finalQueryString.substring("delete".length());
                            Expr deleteCount;
                            if (!finalNamedParameterToIndex.isEmpty()) {
                                Expr parameters = generateParametersObject(finalNamedParameterToIndex, bc, params);

                                // call JpaOperations.delete
                                deleteCount = bc.invokeVirtual(
                                        MethodDesc.of(AbstractManagedJpaOperations.class, "delete", long.class,
                                                Class.class, String.class, Parameters.class),
                                        ops, entityClass,
                                        Const.of(deleteQueryString), parameters);
                            } else {
                                Expr paramsArray = generateParamsArray(queryParameterIndexes, bc, params);

                                // call JpaOperations.delete
                                deleteCount = bc.invokeVirtual(
                                        MethodDesc.of(AbstractManagedJpaOperations.class, "delete", long.class,
                                                Class.class, String.class, Object[].class),
                                        ops, entityClass,
                                        Const.of(deleteQueryString), paramsArray);
                            }
                            handleClearAutomatically(modifyingAnnotation, bc, entityClass);

                            if (DotNames.VOID.equals(methodReturnTypeDotName)) {
                                bc.return_();
                            } else {
                                handleLongReturnValue(bc, deleteCount, methodReturnTypeDotName);
                            }

                        } else if (finalQueryString.toLowerCase().startsWith("update")) {
                            if (!DotNames.PRIMITIVE_INTEGER.equals(methodReturnTypeDotName)
                                    && !DotNames.INTEGER.equals(methodReturnTypeDotName)
                                    && !DotNames.VOID.equals(methodReturnTypeDotName)) {
                                throw new IllegalArgumentException(
                                        method.name() + " of Repository " + repositoryClassInfo
                                                + " is meant to be an update query and can therefore only have a void or integer return type");
                            }

                            Expr updateCount;
                            if (!finalNamedParameterToIndex.isEmpty()) {
                                Expr parameters = generateParametersObject(finalNamedParameterToIndex, bc, params);
                                Expr parametersMap = bc.invokeVirtual(
                                        MethodDesc.of(Parameters.class, "map", Map.class),
                                        parameters);

                                // call JpaOperations.executeUpdate
                                updateCount = bc.invokeVirtual(
                                        MethodDesc.of(AbstractManagedJpaOperations.class, "executeUpdate", int.class,
                                                String.class, Map.class),
                                        ops,
                                        Const.of(finalQueryString),
                                        parametersMap);
                            } else {
                                Expr paramsArray = generateParamsArray(queryParameterIndexes, bc, params);

                                // call JpaOperations.executeUpdate
                                updateCount = bc.invokeVirtual(
                                        MethodDesc.of(AbstractManagedJpaOperations.class, "executeUpdate",
                                                int.class, String.class, Object[].class),
                                        ops,
                                        Const.of(finalQueryString),
                                        paramsArray);
                            }
                            handleClearAutomatically(modifyingAnnotation, bc, entityClass);

                            if (DotNames.VOID.equals(methodReturnTypeDotName)) {
                                bc.return_();
                            } else {
                                handleIntegerReturnValue(bc, updateCount, methodReturnTypeDotName);
                            }

                        } else {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " has been annotated with @Modifying but the @Query does not appear to be " +
                                            "a delete or update query");
                        }
                    } else {
                        // by default just hope that adding select count(*) will do
                        String countQueryString = "SELECT COUNT(*) " + finalQueryString;
                        if (queryInstance != null && queryInstance.value(QUERY_COUNT_FIELD) != null) { // if a countQuery is specified, use it
                            countQueryString = queryInstance.value(QUERY_COUNT_FIELD).asString().trim();
                        } else {
                            // otherwise try and derive the select query from the method name and use that to construct the count query
                            MethodNameParser methodNameParser = new MethodNameParser(repositoryClassInfo, index);
                            try {
                                MethodNameParser.Result parseResult = methodNameParser.parse(method);
                                if (MethodNameParser.QueryType.SELECT == parseResult.getQueryType()) {
                                    countQueryString = "SELECT COUNT (*) " + parseResult.getQuery();
                                }
                            } catch (Exception ignored) {
                                // we just ignore the exception if the method does not match one of the supported styles
                            }
                        }

                        // Find the type of data used in the result
                        // e.g. method.returnType() is a List that may contain non-entity elements
                        Type resultType = verifyQueryResultType(method.returnType(), index);
                        DotName customResultTypeName = resultType.name();

                        if (customResultTypeName.equals(entityClassInfo.name())
                                || customResultTypeName.toString().equals(idTypeStr)
                                || isHibernateSupportedReturnType(customResultTypeName)
                                || getFieldTypeNames(entityClassInfo, entityFieldTypeNames).contains(customResultTypeName)) {
                            // no special handling needed
                            customResultTypeName = null;
                        } else {
                            // The result is using a custom type.
                            List<String> fieldNames = getFieldNames(finalQueryString);

                            // If the custom type is an interface, we need to generate the implementation
                            ClassInfo resultClassInfo = index.getClassByName(customResultTypeName);
                            if (Modifier.isInterface(resultClassInfo.flags())) {
                                // Find the implementation name, and use that for subsequent query result generation
                                customResultTypeName = customResultTypeNames.computeIfAbsent(customResultTypeName,
                                        (k) -> createSimpleInterfaceImpl(k, entityClassInfo.name()));

                                // Remember the parameters for this usage of the custom type, we'll deal with it later
                                customResultTypes.computeIfAbsent(customResultTypeName,
                                        k -> new HashMap<>()).put(methodName, fieldNames);
                            } else {
                                throw new IllegalArgumentException(
                                        "Query annotations may only use interfaces to map results to non-entity types. "
                                                + "Offending query string is \"" + finalQueryString + "\" on method "
                                                + methodName
                                                + " of Repository " + repositoryName);
                            }
                        }

                        Expr panacheQuery;
                        if (!finalNamedParameterToIndex.isEmpty()) {
                            Expr parameters = generateParametersObject(finalNamedParameterToIndex, bc, params);

                            // call JpaOperations.find()
                            panacheQuery = bc.invokeStatic(
                                    MethodDesc.of(AdditionalJpaOperations.class, "find",
                                            PanacheQuery.class, AbstractManagedJpaOperations.class, Class.class, String.class,
                                            String.class, io.quarkus.panache.common.Sort.class, Parameters.class),
                                    ops, entityClass,
                                    Const.of(finalQueryString), Const.of(countQueryString),
                                    generateSort(finalSortParameterIndex, finalPageableParameterIndex, bc, params),
                                    parameters);

                        } else {
                            Expr paramsArray = generateParamsArray(queryParameterIndexes, bc, params);

                            // call JpaOperations.find()
                            panacheQuery = bc.invokeStatic(
                                    MethodDesc.of(AdditionalJpaOperations.class, "find",
                                            PanacheQuery.class, AbstractManagedJpaOperations.class, Class.class, String.class,
                                            String.class, io.quarkus.panache.common.Sort.class, Object[].class),
                                    ops, entityClass,
                                    Const.of(finalQueryString), Const.of(countQueryString),
                                    generateSort(finalSortParameterIndex, finalPageableParameterIndex, bc, params),
                                    paramsArray);
                        }

                        generateFindQueryResultHandling(bc, panacheQuery, finalPageableParameterIndex, params,
                                repositoryClassInfo, entityClassInfo, methodReturnTypeDotName, null, method.name(),
                                customResultTypeName,
                                Object[].class.getName());
                    }
                });
            });
            existingMethods.add(GenerationUtil.methodKey(method.name(), methodReturnTypeDotName.toString(),
                    methodParameterTypesStr));
        }

        for (Map.Entry<DotName, DotName> mapping : customResultTypeNames.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            DotName interfaceName = mapping.getKey();
            DotName implName = mapping.getValue();
            generateCustomResultTypes(interfaceName, implName, customResultTypes.get(implName));
            customClassCreatedCallback.accept(implName.toString());
        }
    }

    private Set<String> extractNamedParameters(String queryString) {
        Set<String> namedParameters = new LinkedHashSet<>();
        final Matcher matcher = NAMED_PARAMETER.matcher(queryString);
        while (matcher.find()) {
            namedParameters.add(matcher.group(1));
        }
        return namedParameters;
    }

    // we currently only support the 'value' attribute of @Query
    private void verifyQueryAnnotation(AnnotationInstance queryInstance, String methodName, String repositoryName) {
        List<AnnotationValue> values = queryInstance.values();
        for (AnnotationValue value : values) {
            if (!QUERY_VALUE_FIELD.equals(value.name()) && !QUERY_COUNT_FIELD.equals(value.name())) {
                throw new IllegalArgumentException("Attribute " + value.name() + " of @Query is currently not supported. " +
                        "Offending method is " + methodName + " of Repository " + repositoryName);
            }
        }
        if (queryInstance.value(QUERY_VALUE_FIELD) == null) {
            throw new IllegalArgumentException("'value' attribute must be specified on @Query annotation of method. " +
                    "Offending method is " + methodName + " of Repository " + repositoryName);
        }
    }

    private Expr generateParamsArray(List<Integer> queryParameterIndexes, BlockCreator bc, ParamVar[] params) {
        LocalVar paramsArray = bc.localVar("paramsArray", bc.newEmptyArray(Object.class, queryParameterIndexes.size()));
        for (int i = 0; i < queryParameterIndexes.size(); i++) {
            bc.set(paramsArray.elem(i), params[queryParameterIndexes.get(i)]);
        }
        return paramsArray;
    }

    private Expr generateParametersObject(Map<String, Integer> namedParameterToIndex, BlockCreator bc, ParamVar[] params) {
        LocalVar parameters = bc.localVar("parameters", bc.new_(ClassDesc.of(Parameters.class.getName())));
        for (Map.Entry<String, Integer> entry : namedParameterToIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            bc.invokeVirtual(
                    MethodDesc.of(Parameters.class, "and", Parameters.class,
                            String.class, Object.class),
                    parameters, Const.of(entry.getKey()), params[entry.getValue()]);
        }
        return parameters;
    }

    // ensure that Sort is correctly handled whether it's specified from the method name or a method param
    private Expr generateSort(Integer sortParameterIndex, Integer pageableParameterIndex, BlockCreator bc, ParamVar[] params) {
        Expr sort = Const.ofNull(ClassDesc.of(io.quarkus.panache.common.Sort.class.getName()));
        if (sortParameterIndex != null) {
            sort = bc.invokeStatic(
                    MethodDesc.of(TypesConverter.class, "toPanacheSort",
                            io.quarkus.panache.common.Sort.class,
                            org.springframework.data.domain.Sort.class),
                    params[sortParameterIndex]);
        } else if (pageableParameterIndex != null) {
            sort = bc.invokeStatic(
                    MethodDesc.of(TypesConverter.class, "pageToPanacheSort",
                            io.quarkus.panache.common.Sort.class,
                            org.springframework.data.domain.Pageable.class),
                    params[pageableParameterIndex]);
        }
        return sort;
    }

    private List<String> getFieldNames(String queryString) {
        Matcher matcher = SELECT_CLAUSE.matcher(queryString);
        if (matcher.find()) {
            String selectClause = matcher.group(1).trim();

            String[] fields = selectClause.split("\\s*,\\s+");
            List<String> fieldNames = new ArrayList<>(fields.length);
            for (String name : fields) {
                Matcher m = FIELD_ALIAS.matcher(name);
                if (m.matches()) {
                    name = m.group(1);
                } else {
                    Matcher n = FIELD_NAME.matcher(name); // (\\w+).*
                    if (n.matches()) {
                        name = n.group(1);
                    }
                }
                fieldNames.add(name.toLowerCase());
            }

            return fieldNames;
        }

        return Collections.emptyList();
    }

    private void generateCustomResultTypes(DotName interfaceName, DotName implName, Map<String, List<String>> queryMethods) {

        ClassInfo interfaceInfo = index.getClassByName(interfaceName);

        Gizmo gizmo = Gizmo.create(nonBeansClassOutput);
        gizmo.class_(implName.toString(), implClassCreator -> {
            implClassCreator.implements_(ClassDesc.of(interfaceName.toString()));

            // Add default constructor
            implClassCreator.defaultConstructor();

            Map<String, FieldDesc> fields = new HashMap<>(3);

            for (MethodInfo method : interfaceInfo.methods()) {
                String getterName = method.name();
                String propertyName = JavaBeanUtil.getPropertyNameFromGetter(getterName);

                Type returnType = method.returnType();
                if (returnType.kind() == Type.Kind.VOID) {
                    throw new IllegalArgumentException("Method " + method.name() + " of interface " + interfaceName
                            + " is not a getter method since it returns void");
                }
                DotName fieldTypeName = getPrimitiveTypeName(returnType.name());

                FieldDesc field = implClassCreator.field(propertyName, ifc -> {
                    ifc.setType(GenerationUtil.toClassDesc(fieldTypeName.toString()));
                });

                // create getter (based on the interface)
                MethodTypeDesc getterMtd = GenerationUtil.toMethodTypeDesc(returnType.name().toString());
                implClassCreator.method(getterName, mc -> {
                    mc.setType(getterMtd);
                    mc.public_();
                    mc.body(bc -> {
                        bc.return_(bc.get(mc.this_().field(field)));
                    });
                });

                fields.put(propertyName.toLowerCase(), field);
            }

            // Add static methods to convert from Object[] to this type
            for (Map.Entry<String, List<String>> queryMethod : queryMethods.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                MethodTypeDesc convertMtd = GenerationUtil.toMethodTypeDesc(implName.toString(), Object[].class.getName());
                implClassCreator.staticMethod("convert_" + queryMethod.getKey(), smc -> {
                    smc.setType(convertMtd);
                    smc.public_();
                    ParamVar arrayParam = smc.parameter("input");

                    smc.body(bc -> {
                        LocalVar newObject = bc.localVar("newObject",
                                bc.new_(ClassDesc.of(implName.toString())));

                        // Use field names in the query-declared order
                        List<String> queryNames = queryMethod.getValue();

                        for (int i = 0; i < queryNames.size(); i++) {
                            FieldDesc f = fields.get(queryNames.get(i));
                            if (f == null) {
                                throw new IllegalArgumentException("@Query annotation for " + queryMethod.getKey()
                                        + " does not use fields from " + interfaceName);
                            } else {
                                bc.set(newObject.field(f),
                                        castReturnValue(bc, arrayParam.elem(i), f.type()));
                            }
                        }
                        bc.return_(newObject);
                    });
                });
            }
        });
    }

    private Expr castReturnValue(BlockCreator bc, Expr resultHandle, ClassDesc type) {
        String typeDesc = type.descriptorString();
        switch (typeDesc) {
            case "I":
                resultHandle = bc.invokeStatic(
                        MethodDesc.of(Integer.class, "valueOf", Integer.class, int.class),
                        resultHandle);
                break;
            case "J":
                resultHandle = bc.invokeStatic(
                        MethodDesc.of(Long.class, "valueOf", Long.class, long.class),
                        resultHandle);
                break;
        }
        return resultHandle;
    }

    private Set<DotName> getFieldTypeNames(ClassInfo entityClassInfo, Set<DotName> entityFieldTypeNames) {
        if (entityFieldTypeNames.isEmpty()) {
            entityClassInfo.fields().stream()
                    .filter(not(fieldInfo -> Modifier.isStatic(fieldInfo.flags())))
                    .filter(not(FieldInfo::isSynthetic))
                    .filter(not(fieldInfo -> fieldInfo.hasAnnotation(DotNames.JPA_TRANSIENT)))
                    .map(fieldInfo -> fieldInfo.type().name())
                    .forEach(entityFieldTypeNames::add);
            // recurse until we reached Object
            Type superClassType = entityClassInfo.superClassType();
            if (superClassType != null && !superClassType.name().equals(DotNames.OBJECT)) {
                var superEntityClassInfo = index.getClassByName(superClassType.name());
                entityFieldTypeNames.addAll(getFieldTypeNames(superEntityClassInfo, new HashSet<>()));
            }
        }
        return entityFieldTypeNames;
    }
}
