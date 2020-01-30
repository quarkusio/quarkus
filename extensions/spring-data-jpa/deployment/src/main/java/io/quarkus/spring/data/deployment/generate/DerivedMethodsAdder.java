package io.quarkus.spring.data.deployment.generate;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.deployment.MethodNameParser;
import io.quarkus.spring.data.runtime.TypesConverter;

public class DerivedMethodsAdder extends AbstractMethodsAdder {

    private final IndexView index;

    public DerivedMethodsAdder(IndexView index) {
        this.index = index;
    }

    public void add(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, ClassInfo repositoryClassInfo, ClassInfo entityClassInfo) {
        MethodNameParser methodNameParser = new MethodNameParser(entityClassInfo, index);
        for (MethodInfo method : repositoryClassInfo.methods()) {
            if (method.annotation(DotNames.SPRING_DATA_QUERY) != null) { // handled by CustomQueryMethodsAdder
                continue;
            }

            if (classCreator.getExistingMethods().contains(GenerationUtil.toMethodDescriptor(generatedClassName, method))) {
                continue;
            }

            Type returnType = method.returnType();

            List<Type> parameters = method.parameters();
            String[] parameterTypesStr = new String[parameters.size()];
            List<Integer> queryParameterIndexes = new ArrayList<>(parameters.size());
            Integer pageableParameterIndex = null;
            Integer sortParameterIndex = null;
            for (int i = 0; i < parameters.size(); i++) {
                DotName parameterType = parameters.get(i).name();
                parameterTypesStr[i] = parameterType.toString();
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

            MethodNameParser.Result parseResult = methodNameParser.parse(method);
            if (parseResult.getParamCount() != queryParameterIndexes.size()) {
                throw new IllegalArgumentException("The number of parameters of method " + method.name() + " of Repository "
                        + repositoryClassInfo
                        + " does not match the number of parameter needed (inferred from the method name)");
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator(method.name(), returnType.name().toString(),
                    parameterTypesStr)) {
                ResultHandle paramsArray = methodCreator.newArray(Object.class, parseResult.getParamCount());
                for (int i = 0; i < queryParameterIndexes.size(); i++) {
                    methodCreator.writeArrayValue(paramsArray, methodCreator.load(i),
                            methodCreator.getMethodParam(queryParameterIndexes.get(i)));
                }

                if (parseResult.getQueryType() == MethodNameParser.QueryType.SELECT) {
                    if (parseResult.getSort() != null && sortParameterIndex != null) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo + " contains both a "
                                        + DotNames.SPRING_DATA_SORT + " parameter and a sort operation");
                    }

                    // ensure that Sort is correctly handled whether it's specified in the method name or via a Sort method param
                    String finalQuery = parseResult.getQuery();
                    ResultHandle sort = methodCreator.loadNull();
                    if (sortParameterIndex != null) {
                        sort = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(TypesConverter.class, "toPanacheSort",
                                        io.quarkus.panache.common.Sort.class,
                                        org.springframework.data.domain.Sort.class),
                                methodCreator.getMethodParam(sortParameterIndex));
                    } else if (parseResult.getSort() != null) {
                        finalQuery += JpaOperations.toOrderBy(parseResult.getSort());
                    } else if (pageableParameterIndex != null) {
                        ResultHandle pageable = methodCreator.getMethodParam(pageableParameterIndex);
                        ResultHandle pageableSort = methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Pageable.class, "getSort", Sort.class),
                                pageable);
                        sort = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(TypesConverter.class, "toPanacheSort",
                                        io.quarkus.panache.common.Sort.class,
                                        org.springframework.data.domain.Sort.class),
                                pageableSort);
                    }

                    // call JpaOperations.find()
                    ResultHandle panacheQuery = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(JpaOperations.class, "find", PanacheQuery.class,
                                    Class.class, String.class, io.quarkus.panache.common.Sort.class, Object[].class),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(finalQuery), sort, paramsArray);

                    generateFindQueryResultHandling(methodCreator, panacheQuery, pageableParameterIndex, repositoryClassInfo,
                            entityClassInfo, returnType.name(), parseResult.getTopCount(), method.name(), null);

                } else if (parseResult.getQueryType() == MethodNameParser.QueryType.COUNT) {
                    if (!DotNames.PRIMITIVE_LONG.equals(returnType.name()) && !DotNames.LONG.equals(returnType.name())) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be a count query and can therefore only have a long return type");
                    }
                    if ((sortParameterIndex != null) || pageableParameterIndex != null) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be a count query and therefore doesn't " +
                                        "support Pageable and Sort method parameters");
                    }

                    // call JpaOperations.count()
                    ResultHandle count = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(JpaOperations.class, "count", long.class,
                                    Class.class, String.class, Object[].class),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(parseResult.getQuery()), paramsArray);

                    handleLongReturnValue(methodCreator, count, returnType.name());

                } else if (parseResult.getQueryType() == MethodNameParser.QueryType.EXISTS) {
                    if (!DotNames.PRIMITIVE_BOOLEAN.equals(returnType.name()) && !DotNames.BOOLEAN.equals(returnType.name())) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be an exists query and can therefore only have a boolean return type");
                    }
                    if ((sortParameterIndex != null) || pageableParameterIndex != null) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be a count query and therefore doesn't " +
                                        "support Pageable and Sort method parameters");
                    }

                    // call JpaOperations.exists()
                    ResultHandle exists = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(JpaOperations.class, "exists", boolean.class,
                                    Class.class, String.class, Object[].class),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(parseResult.getQuery()), paramsArray);

                    handleBooleanReturnValue(methodCreator, exists, returnType.name());

                } else if (parseResult.getQueryType() == MethodNameParser.QueryType.DELETE) {
                    if (!DotNames.PRIMITIVE_LONG.equals(returnType.name()) && !DotNames.LONG.equals(returnType.name())
                            && !DotNames.VOID.equals(returnType.name())) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be a delete query and can therefore only have a void or long return type");
                    }
                    if ((sortParameterIndex != null) || pageableParameterIndex != null) {
                        throw new IllegalArgumentException(
                                method.name() + " of Repository " + repositoryClassInfo
                                        + " is meant to be a delete query and therefore doesn't " +
                                        "support Pageable and Sort method parameters");
                    }
                    methodCreator.addAnnotation(Transactional.class);

                    // call JpaOperations.delete()
                    ResultHandle delete = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(JpaOperations.class, "delete", long.class,
                                    Class.class, String.class, Object[].class),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(parseResult.getQuery()), paramsArray);

                    if (DotNames.VOID.equals(returnType.name())) {
                        methodCreator.returnValue(null);
                    }
                    handleLongReturnValue(methodCreator, delete, returnType.name());
                }
            }
        }
    }
}
