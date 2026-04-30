package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.spring.data.deployment.generate.GenerationUtil.getNamedQueryForMethod;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.deployment.MethodNameParser;
import io.quarkus.spring.data.runtime.TypesConverter;

public class DerivedMethodsAdder extends AbstractMethodsAdder {

    private final IndexView index;
    private final String operationsName;
    private final FieldDesc operationsField;
    private final ClassOutput nonBeansClassOutput;
    private final Consumer<String> projectionClassCreatedCallback;

    public DerivedMethodsAdder(IndexView index, TypeBundle typeBundle, ClassOutput nonBeansClassOutput,
            Consumer<String> projectionClassCreatedCallback) {
        this.index = index;
        operationsName = typeBundle.operations().dotName().toString();
        operationsField = FieldDesc.of(ClassDesc.of(operationsName), "INSTANCE", ClassDesc.of(operationsName));
        this.nonBeansClassOutput = nonBeansClassOutput;
        this.projectionClassCreatedCallback = projectionClassCreatedCallback;
    }

    public void add(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, ClassInfo repositoryClassInfo, ClassInfo entityClassInfo,
            Set<String> existingMethods) {
        MethodNameParser methodNameParser = new MethodNameParser(entityClassInfo, index);
        LinkedHashSet<MethodInfo> repoMethods = new LinkedHashSet<>(repositoryClassInfo.methods());

        // Remember custom return type methods: {resultType:[methodName]}
        Map<DotName, List<String>> customResultTypes = new HashMap<>(3);
        Map<DotName, DotName> customResultTypeImplNames = new HashMap<>(3);

        //As intermediate interfaces are supported for spring data repositories, we need to search the methods declared in such interfaced and add them to the methods to implement list
        for (DotName extendedInterface : repositoryClassInfo.interfaceNames()) {
            addAllMethodOfIntermediateRepository(extendedInterface, repoMethods);
        }
        for (MethodInfo method : repoMethods) {
            if (method.annotation(DotNames.SPRING_DATA_QUERY) != null) { // handled by CustomQueryMethodsAdder
                continue;
            }

            // If method is a named query placed in the entity, we skip it to be handled by CustomQueryMethodsAdder
            if (getNamedQueryForMethod(method, entityClassInfo) != null) {
                continue;
            }

            String methodKey = GenerationUtil.methodKey(generatedClassName, method);
            if (existingMethods.contains(methodKey)) {
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

            // Need effectively final copies for use in lambdas
            final Integer finalPageableParameterIndex = pageableParameterIndex;
            final Integer finalSortParameterIndex = sortParameterIndex;

            MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(returnType.name().toString(), parameterTypesStr);
            classCreator.method(method.name(), mc -> {
                mc.setType(mtd);

                // Add @Transactional for delete queries before calling body()
                if (parseResult.getQueryType() == MethodNameParser.QueryType.DELETE) {
                    mc.addAnnotation(Transactional.class);
                }

                // Declare parameters
                ParamVar[] params = new ParamVar[parameters.size()];
                for (int i = 0; i < parameters.size(); i++) {
                    params[i] = mc.parameter("p" + i);
                }

                mc.body(bc -> {
                    // Store static field and instance field in LocalVars so they can be reused
                    LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                    LocalVar entityClass = bc.localVar("entityClass",
                            bc.get(mc.this_().field(entityClassFieldDescriptor)));

                    // Build params array for query parameters
                    LocalVar paramsArray = bc.localVar("paramsArray",
                            bc.newEmptyArray(Object.class, parseResult.getParamCount()));
                    for (int i = 0; i < queryParameterIndexes.size(); i++) {
                        bc.set(paramsArray.elem(i), params[queryParameterIndexes.get(i)]);
                    }

                    if (parseResult.getQueryType() == MethodNameParser.QueryType.SELECT) {
                        if (parseResult.getSort() != null && finalSortParameterIndex != null) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo + " contains both a "
                                            + DotNames.SPRING_DATA_SORT + " parameter and a sort operation");
                        }

                        // ensure that Sort is correctly handled whether it's specified in the method name or via a Sort method param
                        String finalQuery = parseResult.getQuery();
                        Expr sort = Const.ofNull(ClassDesc.of(io.quarkus.panache.common.Sort.class.getName()));
                        if (finalSortParameterIndex != null) {
                            sort = bc.invokeStatic(
                                    MethodDesc.of(TypesConverter.class, "toPanacheSort",
                                            io.quarkus.panache.common.Sort.class,
                                            org.springframework.data.domain.Sort.class),
                                    params[finalSortParameterIndex]);
                        } else if (parseResult.getSort() != null) {
                            finalQuery += PanacheJpaUtil.toOrderBy(parseResult.getSort());
                        } else if (finalPageableParameterIndex != null) {
                            Expr pageable = params[finalPageableParameterIndex];
                            Expr pageableSort = bc.invokeInterface(
                                    MethodDesc.of(Pageable.class, "getSort", Sort.class),
                                    pageable);
                            sort = bc.invokeStatic(
                                    MethodDesc.of(TypesConverter.class, "toPanacheSort",
                                            io.quarkus.panache.common.Sort.class,
                                            org.springframework.data.domain.Sort.class),
                                    pageableSort);
                        }

                        // call JpaOperations.find()
                        Expr panacheQuery = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "find", Object.class,
                                        Class.class, String.class, io.quarkus.panache.common.Sort.class, Object[].class),
                                ops, entityClass,
                                Const.of(finalQuery), sort, paramsArray);

                        Type resultType = extractResultType(repositoryClassInfo, method);

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
                                        k -> createSimpleInterfaceImpl(k, entityClassInfo.name()));

                                // Remember the parameters for this usage of the custom type, we'll deal with it later
                                customResultTypes.computeIfAbsent(customResultTypeName,
                                        k -> new ArrayList<>()).add(method.name());
                            } else {
                                throw new IllegalArgumentException(
                                        method.name() + " of Repository " + repositoryClassInfo
                                                + " can only use interfaces to map results to non-entity types.");
                            }
                        }

                        generateFindQueryResultHandling(bc, panacheQuery, finalPageableParameterIndex, params,
                                repositoryClassInfo, entityClassInfo, returnType.name(), parseResult.getTopCount(),
                                method.name(), customResultTypeName,
                                entityClassInfo.name().toString());

                    } else if (parseResult.getQueryType() == MethodNameParser.QueryType.COUNT) {
                        if (!DotNames.PRIMITIVE_LONG.equals(returnType.name())
                                && !DotNames.LONG.equals(returnType.name())) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a count query and can therefore only have a long return type");
                        }
                        if ((finalSortParameterIndex != null) || finalPageableParameterIndex != null) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a count query and therefore doesn't " +
                                            "support Pageable and Sort method parameters");
                        }

                        // call JpaOperations.count()
                        Expr count = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "count", long.class,
                                        Class.class, String.class, Object[].class),
                                ops, entityClass,
                                Const.of(parseResult.getQuery()), paramsArray);

                        handleLongReturnValue(bc, count, returnType.name());

                    } else if (parseResult.getQueryType() == MethodNameParser.QueryType.EXISTS) {
                        if (!DotNames.PRIMITIVE_BOOLEAN.equals(returnType.name())
                                && !DotNames.BOOLEAN.equals(returnType.name())) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be an exists query and can therefore only have a boolean return type");
                        }
                        if ((finalSortParameterIndex != null) || finalPageableParameterIndex != null) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a count query and therefore doesn't " +
                                            "support Pageable and Sort method parameters");
                        }

                        // call JpaOperations.exists()
                        Expr exists = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "exists", boolean.class,
                                        Class.class, String.class, Object[].class),
                                ops, entityClass,
                                Const.of(parseResult.getQuery()), paramsArray);

                        handleBooleanReturnValue(bc, exists, returnType.name());

                    } else if (parseResult.getQueryType() == MethodNameParser.QueryType.DELETE) {
                        if (!DotNames.PRIMITIVE_LONG.equals(returnType.name()) && !DotNames.LONG.equals(returnType.name())
                                && !DotNames.VOID.equals(returnType.name())) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a delete query and can therefore only have a void or long return type");
                        }
                        if ((finalSortParameterIndex != null) || finalPageableParameterIndex != null) {
                            throw new IllegalArgumentException(
                                    method.name() + " of Repository " + repositoryClassInfo
                                            + " is meant to be a delete query and therefore doesn't " +
                                            "support Pageable and Sort method parameters");
                        }

                        AnnotationInstance modifyingAnnotation = method.annotation(DotNames.SPRING_DATA_MODIFYING);
                        handleFlushAutomatically(modifyingAnnotation, bc, entityClass);

                        // call JpaOperations.delete()
                        Expr delete = bc.invokeStatic(
                                MethodDesc.of(AdditionalJpaOperations.class, "deleteWithCascade",
                                        long.class, AbstractManagedJpaOperations.class, Class.class, String.class,
                                        Object[].class),
                                ops, entityClass,
                                Const.of(parseResult.getQuery()), paramsArray);

                        handleClearAutomatically(modifyingAnnotation, bc, entityClass);

                        if (DotNames.VOID.equals(returnType.name())) {
                            bc.return_();
                        } else {
                            handleLongReturnValue(bc, delete, returnType.name());
                        }
                    }
                });
            });
            existingMethods.add(methodKey);
        }
        for (Map.Entry<DotName, DotName> mapping : customResultTypeImplNames.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            DotName interfaceName = mapping.getKey();
            DotName implName = mapping.getValue();
            generateCustomResultTypes(interfaceName, implName, entityClassInfo, customResultTypes.get(implName));
            projectionClassCreatedCallback.accept(implName.toString());
        }
    }

    private Type extractResultType(ClassInfo repositoryClassInfo, MethodInfo method) {
        Type resultType = verifyQueryResultType(method.returnType(), index);
        if (resultType.kind() == Type.Kind.TYPE_VARIABLE) {
            // we can handle the generic result type case where interface only declares one generic type that is the same as the method result type uses (TODO: look into enhancing)
            // this is accomplished by resolving the generic type from the interface we are actually implementing
            TypeVariable resultTypeVariable = resultType.asTypeVariable();
            List<TypeVariable> interfaceTypeVariables = method.declaringClass().typeParameters();

            int matchingIndex = -1;
            for (int i = 0; i < interfaceTypeVariables.size(); i++) {
                if (interfaceTypeVariables.get(i).identifier().equals(resultTypeVariable.identifier())) {
                    matchingIndex = i;
                    break;
                }
            }

            if (matchingIndex != -1) {
                List<Type> resolveTypeParameters = JandexUtil.resolveTypeParameters(repositoryClassInfo.name(),
                        method.declaringClass().name(), index);
                if (matchingIndex < resolveTypeParameters.size()) {
                    return resolveTypeParameters.get(matchingIndex);
                }
            }
        }
        return resultType;
    }

    private void addAllMethodOfIntermediateRepository(DotName interfaceDotName, Set<MethodInfo> result) {
        if (GenerationUtil.isIntermediateRepository(interfaceDotName, index)) {
            ClassInfo classInfo = index.getClassByName(interfaceDotName);
            List<MethodInfo> methods = classInfo.methods();
            result.addAll(methods);
            for (DotName superInterface : classInfo.interfaceNames()) {
                addAllMethodOfIntermediateRepository(superInterface, result);
            }
        }
    }

    private void generateCustomResultTypes(DotName interfaceName, DotName implName, ClassInfo entityClassInfo,
            List<String> queryMethods) {

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
                DotName fieldTypeName = returnType.name();

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

                fields.put(getterName, field);
            }

            // Add static methods to convert from entity to this type
            for (String queryMethod : queryMethods) {
                MethodTypeDesc convertMtd = GenerationUtil.toMethodTypeDesc(implName.toString(),
                        entityClassInfo.name().toString());
                implClassCreator.staticMethod("convert_" + queryMethod, smc -> {
                    smc.setType(convertMtd);
                    smc.public_();
                    ParamVar entityParam = smc.parameter("entity");

                    smc.body(bc -> {
                        LocalVar newObject = bc.localVar("newObject",
                                bc.new_(ClassDesc.of(implName.toString())));

                        final List<MethodInfo> availableMethods = availableMethods(entityClassInfo, index);
                        for (Map.Entry<String, FieldDesc> field : fields.entrySet()) {
                            if (!getterExists(availableMethods, field.getKey())) {
                                throw new IllegalArgumentException(field.getKey() + " method does not exists in "
                                        + entityClassInfo.name().toString() + " class.");
                            }

                            FieldDesc f = field.getValue();
                            Expr getterResult = bc.invokeVirtual(
                                    ClassMethodDesc.of(ClassDesc.of(entityClassInfo.name().toString()), field.getKey(),
                                            MethodTypeDesc.of(f.type())),
                                    entityParam);
                            bc.set(newObject.field(f), getterResult);
                        }
                        bc.return_(newObject);
                    });
                });
            }
        });
    }

    private static List<MethodInfo> availableMethods(ClassInfo entityClassInfo, IndexView index) {
        List<MethodInfo> result = new ArrayList<>(entityClassInfo.methods().size());
        while (true) {
            result.addAll(entityClassInfo.methods());
            if (entityClassInfo.superName() == null) {
                break;
            }
            entityClassInfo = index.getClassByName(entityClassInfo.superName());
            if ((entityClassInfo == null) || DotNames.OBJECT.equals(entityClassInfo.name())) {
                break;
            }
        }
        return result;
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
