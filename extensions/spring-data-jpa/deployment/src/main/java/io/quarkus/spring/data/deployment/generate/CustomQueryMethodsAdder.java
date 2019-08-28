package io.quarkus.spring.data.deployment.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.runtime.TypesConverter;

public class CustomQueryMethodsAdder extends AbstractMethodsAdder {

    private static final String QUERY_VALUE_FIELD = "value";

    public void add(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor, ClassInfo repositoryClassInfo,
            ClassInfo entityClassInfo) {
        for (MethodInfo method : repositoryClassInfo.methods()) {
            AnnotationInstance queryInstance = method.annotation(DotNames.SPRING_DATA_QUERY);
            if (queryInstance == null) { // handled by DerivedMethodsAdder
                continue;
            }

            String methodName = method.name();
            String repositoryName = repositoryClassInfo.name().toString();
            String queryString = ensureOnlyValue(queryInstance, methodName, repositoryName);
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
                    ResultHandle panacheQuery;
                    if (useNamedParams) {
                        ResultHandle parameters = generateParametersObject(namedParameterToIndex, methodCreator);

                        // call JpaOperations.find()
                        panacheQuery = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(JpaOperations.class, "find", PanacheQuery.class,
                                        Class.class, String.class, io.quarkus.panache.common.Sort.class, Parameters.class),
                                methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                methodCreator.load(queryString), generateSort(sortParameterIndex, methodCreator), parameters);
                    } else {
                        ResultHandle paramsArray = generateParamsArray(queryParameterIndexes, methodCreator);

                        // call JpaOperations.find()
                        panacheQuery = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(JpaOperations.class, "find", PanacheQuery.class,
                                        Class.class, String.class, io.quarkus.panache.common.Sort.class, Object[].class),
                                methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                                methodCreator.load(queryString), generateSort(sortParameterIndex, methodCreator), paramsArray);
                    }

                    generateFindQueryResultHandling(methodCreator, panacheQuery, pageableParameterIndex, repositoryClassInfo,
                            entityClassInfo, methodReturnTypeDotName, null, method.name());
                }
            }
        }
    }

    // we currently only support the 'value' attribute of @Query
    private String ensureOnlyValue(AnnotationInstance queryInstance, String methodName, String repositoryName) {
        List<AnnotationValue> values = queryInstance.values();
        for (AnnotationValue value : values) {
            if (!QUERY_VALUE_FIELD.equals(value.name())) {
                throw new IllegalArgumentException("Attribute " + value.name() + " of @Query is currently not supported. " +
                        "Offending method is " + methodName + " of Repository " + repositoryName);
            }
        }
        if (queryInstance.value(QUERY_VALUE_FIELD) == null) {
            throw new IllegalArgumentException("'value' attribute must be specified on @Query annotation of method. " +
                    "Offending method is " + methodName + " of Repository " + repositoryName);
        }

        return queryInstance.value(QUERY_VALUE_FIELD).asString().trim();
    }

    private ResultHandle generateParamsArray(List<Integer> queryParameterIndexes, MethodCreator methodCreator) {
        ResultHandle paramsArray = methodCreator.newArray(Object.class,
                methodCreator.load(queryParameterIndexes.size()));
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
}
