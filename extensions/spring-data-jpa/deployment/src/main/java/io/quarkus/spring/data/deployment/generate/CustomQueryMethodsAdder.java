package io.quarkus.spring.data.deployment.generate;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.deployment.MethodNameParser;
import io.quarkus.spring.data.runtime.TypesConverter;

public class CustomQueryMethodsAdder extends AbstractMethodsAdder {

    private static final String QUERY_VALUE_FIELD = "value";
    private static final String QUERY_COUNT_FIELD = "countQuery";

    private static final Pattern SELECT_CLAUSE = Pattern.compile("select\\s+(.+)\\s+from", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_ALIAS = Pattern.compile(".*\\s+[as|AS]+\\s+([\\w\\.]+)");
    private static final Pattern FIELD_NAME = Pattern.compile("(\\w+).*");

    private final IndexView index;
    private final ClassOutput nonBeansClassOutput;
    private final Consumer<String> customClassCreatedCallback;

    public CustomQueryMethodsAdder(IndexView index, ClassOutput classOutput, Consumer<String> customClassCreatedCallback) {
        this.index = index;
        this.nonBeansClassOutput = classOutput;
        this.customClassCreatedCallback = customClassCreatedCallback;
    }

    public void add(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor, ClassInfo repositoryClassInfo,
            ClassInfo entityClassInfo) {

        // Remember custom return types: {resultType:{methodName:[fieldNames]}}
        Map<DotName, Map<String, List<String>>> customResultTypes = new HashMap<>(3);
        Map<DotName, DotName> customResultTypeNames = new HashMap<>(3);

        for (MethodInfo method : repositoryClassInfo.methods()) {

            AnnotationInstance queryInstance = method.annotation(DotNames.SPRING_DATA_QUERY);
            if (queryInstance == null) { // handled by DerivedMethodsAdder
                continue;
            }

            String methodName = method.name();
            String repositoryName = repositoryClassInfo.name().toString();
            verifyQueryAnnotation(queryInstance, methodName, repositoryName);
            String queryString = queryInstance.value(QUERY_VALUE_FIELD).asString().trim();
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

            boolean useNamedParams = (method.annotation(DotNames.SPRING_DATA_PARAM) != null);
            List<Type> methodParameterTypes = method.parameters();
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
                } else if (!useNamedParams) {
                    queryParameterIndexes.add(i);
                }
            }

            // go through the method annotations, find the @Param annotation on parameters
            // and map the name to the method param index
            Map<String, Integer> namedParameterToIndex = new HashMap<>();
            List<AnnotationInstance> annotations = method.annotations();
            for (AnnotationInstance annotation : annotations) {
                if ((annotation.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER)
                        || (!DotNames.SPRING_DATA_PARAM.equals(annotation.name()))) {
                    continue;
                }
                namedParameterToIndex.put(annotation.value().asString(),
                        (int) annotation.target().asMethodParameter().position());
            }

            boolean isModifying = (method.annotation(DotNames.SPRING_DATA_MODIFYING) != null);
            if (isModifying && (sortParameterIndex != null || pageableParameterIndex != null)) {
                throw new IllegalArgumentException(
                        method.name() + " of Repository " + repositoryClassInfo
                                + " is meant to be a insert/update/delete query and therefore doesn't " +
                                "support Pageable and Sort method parameters");
            }

            DotName methodReturnTypeDotName = method.returnType().name();

            try (MethodCreator methodCreator = classCreator.getMethodCreator(method.name(), methodReturnTypeDotName.toString(),
                    methodParameterTypesStr)) {

                if (isModifying) {
                    methodCreator.addAnnotation(Transactional.class);
                    if (queryString.toLowerCase().startsWith("delete")) {
                        if (!DotNames.PRIMITIVE_LONG.equals(methodReturnTypeDotName)
                                && !DotNames.LONG.equals(methodReturnTypeDotName)
                                && !DotNames.VOID.equals(methodReturnTypeDotName)) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a delete query and can therefore only have a void or long return type");
                        }

                        // we need to strip 'delete' or else JpaOperations.delete will generate the wrong query
                        String deleteQueryString = queryString.substring("delete".length());
                        ResultHandle deleteCount;
                        if (useNamedParams) {
                            ResultHandle parameters = generateParametersObject(namedParameterToIndex, methodCreator);

                            // call JpaOperations.delete
                            deleteCount = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(JpaOperations.class, "delete", long.class,
                                            Class.class, String.class, Parameters.class),
                                    methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                    methodCreator.load(deleteQueryString), parameters);
                        } else {
                            ResultHandle paramsArray = generateParamsArray(queryParameterIndexes, methodCreator);

                            // call JpaOperations.delete
                            deleteCount = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(JpaOperations.class, "delete", long.class,
                                            Class.class, String.class, Object[].class),
                                    methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                    methodCreator.load(deleteQueryString), paramsArray);
                        }

                        if (DotNames.VOID.equals(methodReturnTypeDotName)) {
                            methodCreator.returnValue(null);
                        }
                        handleLongReturnValue(methodCreator, deleteCount, methodReturnTypeDotName);

                    } else if (queryString.toLowerCase().startsWith("update")) {
                        if (!DotNames.PRIMITIVE_INTEGER.equals(methodReturnTypeDotName)
                                && !DotNames.INTEGER.equals(methodReturnTypeDotName)
                                && !DotNames.VOID.equals(methodReturnTypeDotName)) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be an update query and can therefore only have a void or integer return type");
                        }

                        ResultHandle updateCount;
                        if (useNamedParams) {
                            ResultHandle parameters = generateParametersObject(namedParameterToIndex, methodCreator);
                            ResultHandle parametersMap = methodCreator.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(Parameters.class, "map", Map.class),
                                    parameters);

                            // call JpaOperations.executeUpdate
                            updateCount = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(JpaOperations.class, "executeUpdate", int.class,
                                            String.class, Map.class),
                                    methodCreator.load(queryString), parametersMap);
                        } else {
                            ResultHandle paramsArray = generateParamsArray(queryParameterIndexes, methodCreator);

                            // call JpaOperations.executeUpdate
                            updateCount = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(JpaOperations.class, "executeUpdate", int.class,
                                            String.class, Object[].class),
                                    methodCreator.load(queryString), paramsArray);
                        }

                        if (DotNames.VOID.equals(methodReturnTypeDotName)) {
                            methodCreator.returnValue(null);
                        }
                        handleIntegerReturnValue(methodCreator, updateCount, methodReturnTypeDotName);

                    } else {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " has been annotated with @Modifying but the @Query does not appear to be " +
                                        "a delete or update query");
                    }
                } else {
                    // by default just hope that adding select count(*) will do
                    String countQueryString = "SELECT COUNT(*) " + queryString;
                    if (queryInstance.value(QUERY_COUNT_FIELD) != null) { // if a countQuery is specified, use it
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
                    Type resultType = verifyQueryResultType(method.returnType());
                    DotName customResultTypeName = resultType.name();

                    if (customResultTypeName.equals(entityClassInfo.name())
                            || customResultTypeName.equals(DotNames.OBJECT)
                            || isIntLongOrBoolean(customResultTypeName)) {
                        // no special handling needed
                        customResultTypeName = null;
                    } else {
                        // The result is using a custom type.
                        List<String> fieldNames = getFieldNames(queryString);

                        // If the custom type is an interface, we need to generate the implementation
                        ClassInfo resultClassInfo = index.getClassByName(customResultTypeName);
                        if (Modifier.isInterface(resultClassInfo.flags())) {
                            // Find the implementation name, and use that for subsequent query result generation
                            customResultTypeName = customResultTypeNames.computeIfAbsent(customResultTypeName,
                                    k -> createImplDotName(resultType.name()));

                            // Remember the parameters for this usage of the custom type, we'll deal with it later
                            customResultTypes.computeIfAbsent(customResultTypeName,
                                    k -> new HashMap<String, List<String>>()).put(methodName, fieldNames);
                        } else {
                            throw new IllegalArgumentException(
                                    "Query annotations may only use interfaces to map results to non-entity types. "
                                            + "Offending query string is \"" + queryString + "\" on method " + methodName
                                            + " of Repository " + repositoryName);
                        }
                    }

                    ResultHandle panacheQuery;
                    if (useNamedParams) {
                        ResultHandle parameters = generateParametersObject(namedParameterToIndex, methodCreator);

                        // call JpaOperations.find()
                        panacheQuery = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(AdditionalJpaOperations.class, "find", PanacheQuery.class,
                                        Class.class, String.class, String.class, io.quarkus.panache.common.Sort.class,
                                        Parameters.class),
                                methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                methodCreator.load(queryString), methodCreator.load(countQueryString),
                                generateSort(sortParameterIndex, methodCreator), parameters);

                    } else {
                        ResultHandle paramsArray = generateParamsArray(queryParameterIndexes, methodCreator);

                        // call JpaOperations.find()
                        panacheQuery = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(AdditionalJpaOperations.class, "find", PanacheQuery.class,
                                        Class.class, String.class, String.class, io.quarkus.panache.common.Sort.class,
                                        Object[].class),
                                methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                methodCreator.load(queryString), methodCreator.load(countQueryString),
                                generateSort(sortParameterIndex, methodCreator), paramsArray);
                    }

                    generateFindQueryResultHandling(methodCreator, panacheQuery, pageableParameterIndex, repositoryClassInfo,
                            entityClassInfo, methodReturnTypeDotName, null, method.name(), customResultTypeName);
                }

            }
        }

        for (Map.Entry<DotName, DotName> mapping : customResultTypeNames.entrySet()) {
            DotName interfaceName = mapping.getKey();
            DotName implName = mapping.getValue();
            generateCustomResultTypes(interfaceName, implName, customResultTypes.get(implName));
            customClassCreatedCallback.accept(implName.toString());
        }
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

    private ResultHandle generateParamsArray(List<Integer> queryParameterIndexes, MethodCreator methodCreator) {
        ResultHandle paramsArray = methodCreator.newArray(Object.class, queryParameterIndexes.size());
        for (int i = 0; i < queryParameterIndexes.size(); i++) {
            methodCreator.writeArrayValue(paramsArray, methodCreator.load(i),
                    methodCreator.getMethodParam(queryParameterIndexes.get(i)));
        }
        return paramsArray;
    }

    private ResultHandle generateParametersObject(Map<String, Integer> namedParameterToIndex, MethodCreator methodCreator) {
        ResultHandle parameters = methodCreator.newInstance(MethodDescriptor.ofConstructor(Parameters.class));
        for (Map.Entry<String, Integer> entry : namedParameterToIndex.entrySet()) {
            methodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Parameters.class, "and", Parameters.class,
                            String.class, Object.class),
                    parameters, methodCreator.load(entry.getKey()), methodCreator.getMethodParam(entry.getValue()));
        }
        return parameters;
    }

    // ensure that Sort is correctly handled whether it's specified from the method name or a method param
    private ResultHandle generateSort(Integer sortParameterIndex, MethodCreator methodCreator) {
        ResultHandle sort = methodCreator.loadNull();
        if (sortParameterIndex != null) {
            sort = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(TypesConverter.class, "toPanacheSort",
                            io.quarkus.panache.common.Sort.class,
                            org.springframework.data.domain.Sort.class),
                    methodCreator.getMethodParam(sortParameterIndex));
        }
        return sort;
    }

    // Make sure the return type is referencing a class we have indexed 
    // somewhere along the way (e.g. non-entity return types in @Query methods)
    // Unless it is some kind of collection containing multiple types, 
    // return the type used in the query result.
    private Type verifyQueryResultType(Type t) {
        if (isIntLongOrBoolean(t.name())) {
            return t;
        }
        if (t.kind() == Kind.ARRAY) {
            return verifyQueryResultType(t.asArrayType().component());
        } else if (t.kind() == Kind.PARAMETERIZED_TYPE) {
            List<Type> list = t.asParameterizedType().arguments();
            if (list.size() == 1) {
                return verifyQueryResultType(list.get(0));
            } else {
                for (Type x : list) {
                    verifyQueryResultType(x);
                }
                return t;
            }
        } else if (!DotNames.OBJECT.equals(t.name())) {
            ClassInfo typeClassInfo = index.getClassByName(t.name());
            if (typeClassInfo == null) {
                throw new IllegalStateException(t.name() + " was not part of the Quarkus index");
            }
        }
        return t;
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

    private DotName createImplDotName(DotName ifaceName) {
        String fullName = ifaceName.toString();

        // package name: must be in the same package as the interface
        final int index = fullName.lastIndexOf('.');
        String packageName = "";
        if (index > 0 && index < fullName.length() - 1) {
            packageName = fullName.substring(0, index) + ".";
        }

        return DotName.createSimple(packageName
                + (ifaceName.isInner() ? ifaceName.local() : ifaceName.withoutPackagePrefix()) + "_"
                + HashUtil.sha1(ifaceName.toString()));
    }

    private void generateCustomResultTypes(DotName interfaceName, DotName implName, Map<String, List<String>> queryMethods) {

        ClassInfo interfaceInfo = index.getClassByName(interfaceName);

        try (ClassCreator implClassCreator = ClassCreator.builder().classOutput(nonBeansClassOutput)
                .interfaces(interfaceName.toString()).className(implName.toString())
                .build()) {

            Map<String, FieldDescriptor> fields = new HashMap<>(3);

            for (MethodInfo method : interfaceInfo.methods()) {
                String getterName = method.name();
                String propertyName = JavaBeanUtil.getPropertyNameFromGetter(getterName);

                Type returnType = method.returnType();
                if (returnType.kind() == Type.Kind.VOID) {
                    throw new IllegalArgumentException("Method " + method.name() + " of interface " + interfaceName
                            + " is not a getter method since it returns void");
                }
                DotName fieldTypeName = getPrimitiveTypeName(returnType.name());

                FieldDescriptor field = implClassCreator.getFieldCreator(propertyName, fieldTypeName.toString())
                        .getFieldDescriptor();

                // create getter (based on the interface)
                try (MethodCreator getter = implClassCreator.getMethodCreator(getterName, returnType.toString())) {
                    getter.setModifiers(Modifier.PUBLIC);
                    getter.returnValue(getter.readInstanceField(field, getter.getThis()));
                }

                fields.put(propertyName.toLowerCase(), field);
            }

            // Add static methods to convert from Object[] to this type
            for (Map.Entry<String, List<String>> queryMethod : queryMethods.entrySet()) {
                try (MethodCreator convert = implClassCreator.getMethodCreator("convert_" + queryMethod.getKey(),
                        implName.toString(), Object[].class.getName())) {
                    convert.setModifiers(Modifier.STATIC);

                    ResultHandle newObject = convert.newInstance(MethodDescriptor.ofConstructor(implName.toString()));

                    // Use field names in the query-declared order
                    List<String> queryNames = queryMethod.getValue();

                    // Object[] is the only paramter: values are in column/declared order
                    ResultHandle array = convert.getMethodParam(0);

                    for (int i = 0; i < queryNames.size(); i++) {
                        FieldDescriptor f = fields.get(queryNames.get(i));
                        if (f == null) {
                            throw new IllegalArgumentException("@Query annotation for " + queryMethod.getKey()
                                    + " does not use fields from " + interfaceName);
                        } else {
                            convert.writeInstanceField(f, newObject,
                                    castReturnValue(convert, convert.readArrayValue(array, i), f.getType()));
                        }
                    }
                    convert.returnValue(newObject);
                }
            }
        }
    }

    private ResultHandle castReturnValue(MethodCreator methodCreator, ResultHandle resultHandle, String type) {
        switch (type) {
            case "I":
                resultHandle = methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                        resultHandle);
                break;
            case "J":
                resultHandle = methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, long.class),
                        resultHandle);
                break;
        }
        return resultHandle;
    }

    private DotName getPrimitiveTypeName(DotName returnTypeName) {
        if (DotNames.LONG.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_LONG;
        }
        if (DotNames.INTEGER.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_INTEGER;
        }
        if (DotNames.BOOLEAN.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_BOOLEAN;
        }
        return returnTypeName;
    }

}
