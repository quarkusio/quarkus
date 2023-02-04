package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.spring.data.deployment.generate.GenerationUtil.getNamedQueryForMethod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.deployment.MethodNameParser;
import io.quarkus.spring.data.runtime.TypesConverter;

public class DerivedMethodsAdder extends AbstractMethodsAdder {

    private final IndexView index;
    private final String operationsName;
    private final FieldDescriptor operationsField;
    private final ClassOutput nonBeansClassOutput;
    private final Consumer<String> projectionClassCreatedCallback;

    public DerivedMethodsAdder(IndexView index, TypeBundle typeBundle, ClassOutput nonBeansClassOutput,
            Consumer<String> projectionClassCreatedCallback) {
        this.index = index;
        operationsName = typeBundle.operations().dotName().toString();
        operationsField = of(operationsName, "INSTANCE", operationsName);
        this.nonBeansClassOutput = nonBeansClassOutput;
        this.projectionClassCreatedCallback = projectionClassCreatedCallback;
    }

    public void add(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, ClassInfo repositoryClassInfo, ClassInfo entityClassInfo) {
        MethodNameParser methodNameParser = new MethodNameParser(entityClassInfo, index);
        List<MethodInfo> repoMethods = new ArrayList<>(repositoryClassInfo.methods());

        // Remember custom return type methods: {resultType:[methodName]}
        Map<DotName, List<String>> customResultTypes = new HashMap<>(3);
        Map<DotName, DotName> customResultTypeImplNames = new HashMap<>(3);

        //As intermediate interfaces are supported for spring data repositories, we need to search the methods declared in such interfaced and add them to the methods to implement list
        for (DotName extendedInterface : repositoryClassInfo.interfaceNames()) {
            if (GenerationUtil.isIntermediateRepository(extendedInterface, index)) {
                List<MethodInfo> methods = index.getClassByName(extendedInterface).methods();
                repoMethods.addAll(methods);
            }
        }
        for (MethodInfo method : repoMethods) {
            if (method.annotation(DotNames.SPRING_DATA_QUERY) != null) { // handled by CustomQueryMethodsAdder
                continue;
            }

            // If method is a named query placed in the entity, we skip it to be handled by CustomQueryMethodsAdder
            if (getNamedQueryForMethod(method, entityClassInfo) != null) {
                continue;
            }

            if (classCreator.getExistingMethods().contains(GenerationUtil.toMethodDescriptor(generatedClassName, method))) {
                continue;
            }

            if (!Modifier.isAbstract(method.flags())) { // skip defaults methods
                continue;
            }

            Type returnType = method.returnType();

            List<Type> parameters = method.parameterTypes();
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
                        finalQuery += PanacheJpaUtil.toOrderBy(parseResult.getSort());
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
                    ResultHandle panacheQuery = methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "find", Object.class,
                                    Class.class, String.class, io.quarkus.panache.common.Sort.class, Object[].class),
                            methodCreator.readStaticField(operationsField),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(finalQuery), sort, paramsArray);

                    Type resultType = verifyQueryResultType(method.returnType(), index);
                    DotName customResultTypeName = resultType.name();

                    if (customResultTypeName.equals(entityClassInfo.name())
                            || isHibernateSupportedReturnType(customResultTypeName)) {
                        // no special handling needed
                        customResultTypeName = null;
                    } else {
                        // If the custom type is an interface, we need to generate the implementation
                        ClassInfo resultClassInfo = index.getClassByName(customResultTypeName);
                        if (Modifier.isInterface(resultClassInfo.flags())) {
                            // Find the implementation name, and use that for subsequent query result generation
                            customResultTypeName = customResultTypeImplNames.computeIfAbsent(customResultTypeName,
                                    k -> createSimpleInterfaceImpl(resultType.name()));

                            // Remember the parameters for this usage of the custom type, we'll deal with it later
                            customResultTypes.computeIfAbsent(customResultTypeName,
                                    k -> new ArrayList<>()).add(method.name());
                        } else {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " can only use interfaces to map results to non-entity types.");
                        }
                    }

                    generateFindQueryResultHandling(methodCreator, panacheQuery, pageableParameterIndex, repositoryClassInfo,
                            entityClassInfo, returnType.name(), parseResult.getTopCount(), method.name(), customResultTypeName,
                            entityClassInfo.name().toString());

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
                    ResultHandle count = methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "count", long.class,
                                    Class.class, String.class, Object[].class),
                            methodCreator.readStaticField(operationsField),
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
                    ResultHandle exists = methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "exists", boolean.class,
                                    Class.class, String.class, Object[].class),
                            methodCreator.readStaticField(operationsField),
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

                    AnnotationInstance modifyingAnnotation = method.annotation(DotNames.SPRING_DATA_MODIFYING);
                    handleFlushAutomatically(modifyingAnnotation, methodCreator, entityClassFieldDescriptor);

                    // call JpaOperations.delete()
                    ResultHandle delete = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(AdditionalJpaOperations.class, "deleteWithCascade",
                                    long.class, AbstractJpaOperations.class, Class.class, String.class, Object[].class),
                            methodCreator.readStaticField(operationsField),
                            methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()),
                            methodCreator.load(parseResult.getQuery()), paramsArray);

                    handleClearAutomatically(modifyingAnnotation, methodCreator, entityClassFieldDescriptor);

                    if (DotNames.VOID.equals(returnType.name())) {
                        methodCreator.returnValue(null);
                    }
                    handleLongReturnValue(methodCreator, delete, returnType.name());
                }
            }
        }
        for (Map.Entry<DotName, DotName> mapping : customResultTypeImplNames.entrySet()) {
            DotName interfaceName = mapping.getKey();
            DotName implName = mapping.getValue();
            generateCustomResultTypes(interfaceName, implName, entityClassInfo, customResultTypes.get(implName));
            projectionClassCreatedCallback.accept(implName.toString());
        }
    }

    private void generateCustomResultTypes(DotName interfaceName, DotName implName, ClassInfo entityClassInfo,
            List<String> queryMethods) {

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

                fields.put(getterName, field);
            }

            // Add static methods to convert from Object[] to this type
            for (String queryMethod : queryMethods) {
                try (MethodCreator convert = implClassCreator.getMethodCreator("convert_" + queryMethod,
                        implName.toString(), entityClassInfo.name().toString())) {
                    convert.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

                    ResultHandle newObject = convert.newInstance(MethodDescriptor.ofConstructor(implName.toString()));

                    ResultHandle entity = convert.getMethodParam(0);
                    final List<MethodInfo> availableMethods = entityClassInfo.methods();
                    for (Map.Entry<String, FieldDescriptor> field : fields.entrySet()) {
                        if (!getterExists(availableMethods, field.getKey())) {
                            throw new IllegalArgumentException(field.getKey() + " method does not exists in "
                                    + entityClassInfo.name().toString() + " class.");
                        }

                        FieldDescriptor f = field.getValue();
                        convert.writeInstanceField(f, newObject, convert.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(entityClassInfo.name().toString(), field.getKey(), f.getType()),
                                entity));
                    }
                    convert.returnValue(newObject);
                }
            }
        }
    }

    private boolean getterExists(List<MethodInfo> methods, String getterName) {
        for (MethodInfo method : methods) {
            if (method.name().equals(getterName)) {
                return true;
            }
        }
        return false;
    }
}
