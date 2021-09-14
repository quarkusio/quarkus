package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.gizmo.FieldDescriptor.of;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.runtime.FunctionalityNotImplemented;
import io.quarkus.spring.data.runtime.RepositorySupport;
import io.quarkus.spring.data.runtime.TypesConverter;

public class StockMethodsAdder {

    private static Set<MethodInfo> ALL_SPRING_DATA_REPOSITORY_METHODS = null;

    private final IndexView index;
    private final FieldDescriptor operationsField;

    public StockMethodsAdder(IndexView index, TypeBundle typeBundle) {
        this.index = index;
        String operationsName = typeBundle.operations().dotName().toString();
        operationsField = of(operationsName, "INSTANCE", operationsName);
    }

    public void add(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, ClassInfo repositoryToImplement, DotName entityDotName, String idTypeStr) {

        Set<MethodInfo> methodsOfExtendedSpringDataRepositories = methodsOfExtendedSpringDataRepositories(
                repositoryToImplement);
        Set<MethodInfo> stockMethodsAddedToInterface = stockMethodsAddedToInterface(repositoryToImplement);
        Set<MethodInfo> allMethodsToBeImplemented = new HashSet<>(methodsOfExtendedSpringDataRepositories);
        allMethodsToBeImplemented.addAll(stockMethodsAddedToInterface);

        Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult = new HashMap<>();
        for (MethodInfo methodInfo : allMethodsToBeImplemented) {
            allMethodsToBeImplementedToResult.put(GenerationUtil.toMethodDescriptor(generatedClassName, methodInfo), false);
        }

        String entityTypeStr = entityDotName.toString();

        // for all Spring Data repository methods we know how to implement, check if the generated class actually needs the method
        // and if so generate the implementation while also keeping the proper records

        generateSave(classCreator, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateSaveAndFlush(classCreator, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateSaveAll(classCreator, entityClassFieldDescriptor, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateFlush(classCreator, generatedClassName, allMethodsToBeImplementedToResult);
        generateFindById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult);
        generateExistsById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult);
        generateGetOne(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult);
        generateFindAll(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateFindAllWithSort(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateFindAllWithPageable(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult);
        generateFindAllById(classCreator, entityClassFieldDescriptor, generatedClassName, entityDotName, entityTypeStr,
                idTypeStr, allMethodsToBeImplementedToResult);
        generateCount(classCreator, entityClassFieldDescriptor, generatedClassName, allMethodsToBeImplementedToResult);
        generateDeleteById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult);
        generateDelete(classCreator, generatedClassName, entityTypeStr, allMethodsToBeImplementedToResult);
        generateDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, allMethodsToBeImplementedToResult);
        generateDeleteAll(classCreator, entityClassFieldDescriptor, generatedClassName, allMethodsToBeImplementedToResult);
        generateDeleteAllInBatch(classCreator, entityClassFieldDescriptor, generatedClassName,
                allMethodsToBeImplementedToResult);

        handleUnimplementedMethods(classCreator, allMethodsToBeImplementedToResult);
    }

    private void generateSave(ClassCreator classCreator, String generatedClassName,
            DotName entityDotName, String entityTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor saveDescriptor = MethodDescriptor.ofMethod(generatedClassName, "save", entityTypeStr,
                entityTypeStr);
        MethodDescriptor bridgeSaveDescriptor = MethodDescriptor.ofMethod(generatedClassName, "save", Object.class,
                Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(saveDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveDescriptor)) {

            if (!classCreator.getExistingMethods().contains(saveDescriptor)) {
                try (MethodCreator save = classCreator.getMethodCreator(saveDescriptor)) {
                    save.addAnnotation(Transactional.class);

                    ResultHandle entity = save.getMethodParam(0);

                    // if an entity is Persistable, then all we need to do is call isNew to determine if it's new or not
                    if (isPersistable(entityDotName)) {
                        ResultHandle isNew = save.invokeVirtualMethod(
                                ofMethod(entityDotName.toString(), "isNew", boolean.class.toString()),
                                entity);
                        BranchResult isNewBranch = save.ifTrue(isNew);
                        generatePersistAndReturn(entity, isNewBranch.trueBranch());
                        generateMergeAndReturn(entity, isNewBranch.falseBranch());
                    } else {
                        AnnotationTarget idAnnotationTarget = getIdAnnotationTarget(entityDotName, index);
                        ResultHandle idValue = generateObtainValue(save, entityDotName, entity, idAnnotationTarget);
                        Type idType = getTypeOfTarget(idAnnotationTarget);
                        Optional<AnnotationTarget> versionValueTarget = getVersionAnnotationTarget(entityDotName, index);

                        // the following code generated bytecode that:
                        // if there is a field annotated with @Version, calls 'persist' if the field is null, 'merge' otherwise
                        // if there is no field annotated with @Version, then if the value of the field annotated with '@Id'
                        // is "falsy", 'persist' is called, otherwise 'merge' is called

                        if (versionValueTarget.isPresent()) {
                            Type versionType = getTypeOfTarget(versionValueTarget.get());
                            if (versionType instanceof PrimitiveType) {
                                throw new IllegalArgumentException(
                                        "The '@Version' annotation cannot be used on primitive types. Offending entity is '"
                                                + entityDotName + "'.");
                            }
                            ResultHandle versionValue = generateObtainValue(save, entityDotName, entity,
                                    versionValueTarget.get());
                            BranchResult versionValueIsNullBranch = save.ifNull(versionValue);
                            generatePersistAndReturn(entity, versionValueIsNullBranch.trueBranch());
                            generateMergeAndReturn(entity, versionValueIsNullBranch.falseBranch());
                        }

                        BytecodeCreator idValueUnset;
                        BytecodeCreator idValueSet;
                        if (idType instanceof PrimitiveType) {
                            if (!idType.name().equals(DotNames.PRIMITIVE_LONG)
                                    && !idType.name().equals(DotNames.PRIMITIVE_INTEGER)) {
                                throw new IllegalArgumentException("Id type of '" + entityDotName + "' is invalid.");
                            }
                            if (idType.name().equals(DotNames.PRIMITIVE_LONG)) {
                                ResultHandle longObject = save.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, long.class), idValue);
                                idValue = save.invokeVirtualMethod(MethodDescriptor.ofMethod(Long.class, "intValue", int.class),
                                        longObject);
                            }
                            BranchResult idValueNonZeroBranch = save.ifNonZero(idValue);
                            idValueSet = idValueNonZeroBranch.trueBranch();
                            idValueUnset = idValueNonZeroBranch.falseBranch();
                        } else {
                            BranchResult idValueNullBranch = save.ifNull(idValue);
                            idValueSet = idValueNullBranch.falseBranch();
                            idValueUnset = idValueNullBranch.trueBranch();
                        }
                        generatePersistAndReturn(entity, idValueUnset);
                        generateMergeAndReturn(entity, idValueSet);
                    }
                }
                try (MethodCreator bridgeSave = classCreator.getMethodCreator(bridgeSaveDescriptor)) {
                    MethodDescriptor save = MethodDescriptor.ofMethod(generatedClassName, "save", entityTypeStr,
                            entityTypeStr);
                    ResultHandle methodParam = bridgeSave.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeSave.checkCast(methodParam, entityTypeStr);
                    ResultHandle result = bridgeSave.invokeVirtualMethod(save, bridgeSave.getThis(), castedMethodParam);
                    bridgeSave.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(saveDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveDescriptor, true);
        }
    }

    private boolean isPersistable(DotName entityDotName) {
        ClassInfo classInfo = index.getClassByName(entityDotName);
        if (classInfo == null) {
            throw new IllegalStateException("Entity " + entityDotName + " was not part of the Quarkus index");
        }

        if (classInfo.interfaceNames().contains(DotNames.SPRING_DATA_PERSISTABLE)) {
            return true;
        }

        DotName superDotName = classInfo.superName();
        if (superDotName.equals(DotNames.OBJECT)) {
            return false;
        }

        return isPersistable(superDotName);
    }

    private void generatePersistAndReturn(ResultHandle entity, BytecodeCreator bytecodeCreator) {
        bytecodeCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(AbstractJpaOperations.class, "persist", void.class, Object.class),
                bytecodeCreator.readStaticField(operationsField),
                entity);
        bytecodeCreator.returnValue(entity);
    }

    private void generateMergeAndReturn(ResultHandle entity, BytecodeCreator bytecodeCreator) {
        ResultHandle entityManager = bytecodeCreator.invokeVirtualMethod(
                ofMethod(AbstractJpaOperations.class, "getEntityManager", EntityManager.class),
                bytecodeCreator.readStaticField(operationsField));
        entity = bytecodeCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(EntityManager.class, "merge", Object.class, Object.class),
                entityManager, entity);
        bytecodeCreator.returnValue(entity);
    }

    /**
     * Given an annotation target, generate the bytecode that is needed to obtain its value
     * either by reading the field or by calling the method.
     * Meant to be called for annotations alike {@code @Id} or {@code @Version}
     */
    private ResultHandle generateObtainValue(MethodCreator methodCreator, DotName entityDotName, ResultHandle entity,
            AnnotationTarget annotationTarget) {
        if (annotationTarget instanceof FieldInfo) {
            FieldInfo fieldInfo = annotationTarget.asField();
            if (Modifier.isPublic(fieldInfo.flags())) {
                return methodCreator.readInstanceField(of(fieldInfo), entity);
            }

            String getterMethodName = JavaBeanUtil.getGetterName(fieldInfo.name(), fieldInfo.type().name());
            return methodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(entityDotName.toString(), getterMethodName, fieldInfo.type().name().toString()),
                    entity);
        }
        MethodInfo methodInfo = annotationTarget.asMethod();
        return methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(entityDotName.toString(), methodInfo.name(),
                        methodInfo.returnType().name().toString()),
                entity);
    }

    private Type getTypeOfTarget(AnnotationTarget idAnnotationTarget) {
        if (idAnnotationTarget instanceof FieldInfo) {
            return idAnnotationTarget.asField().type();
        }
        return idAnnotationTarget.asMethod().returnType();
    }

    private void generateSaveAndFlush(ClassCreator classCreator,
            String generatedClassName, DotName entityDotName, String entityTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor saveAndFlushDescriptor = MethodDescriptor.ofMethod(generatedClassName, "saveAndFlush", entityTypeStr,
                entityTypeStr);
        MethodDescriptor bridgeSaveAndFlushDescriptor = MethodDescriptor.ofMethod(generatedClassName, "saveAndFlush",
                Object.class,
                Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(saveAndFlushDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveAndFlushDescriptor)) {

            if (!classCreator.getExistingMethods().contains(saveAndFlushDescriptor)) {
                MethodDescriptor save = MethodDescriptor.ofMethod(generatedClassName, "save", entityTypeStr,
                        entityTypeStr);

                // we need to force the generation of findById since this method depends on it
                allMethodsToBeImplementedToResult.put(save, false);
                generateSave(classCreator, generatedClassName, entityDotName, entityTypeStr,
                        allMethodsToBeImplementedToResult);

                try (MethodCreator saveAndFlush = classCreator.getMethodCreator(saveAndFlushDescriptor)) {
                    saveAndFlush.addAnnotation(Transactional.class);

                    ResultHandle entity = saveAndFlush.getMethodParam(0);
                    entity = saveAndFlush.invokeVirtualMethod(save, saveAndFlush.getThis(), entity);
                    saveAndFlush.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "flush", void.class),
                            saveAndFlush.readStaticField(operationsField));
                    saveAndFlush.returnValue(entity);
                }
                try (MethodCreator bridgeSave = classCreator.getMethodCreator(bridgeSaveAndFlushDescriptor)) {
                    MethodDescriptor saveAndFlush = MethodDescriptor.ofMethod(generatedClassName, "saveAndFlush", entityTypeStr,
                            entityTypeStr);
                    ResultHandle methodParam = bridgeSave.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeSave.checkCast(methodParam, entityTypeStr);
                    ResultHandle result = bridgeSave.invokeVirtualMethod(saveAndFlush, bridgeSave.getThis(), castedMethodParam);
                    bridgeSave.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(saveAndFlushDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveAndFlushDescriptor, true);
        }
    }

    private void generateSaveAll(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, DotName entityDotName, String entityTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor saveAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "saveAll", List.class,
                Iterable.class);
        MethodDescriptor bridgeSaveAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "saveAll", Iterable.class,
                Iterable.class);

        if (allMethodsToBeImplementedToResult.containsKey(saveAllDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveAllDescriptor)) {

            if (!classCreator.getExistingMethods().contains(saveAllDescriptor)) {
                MethodDescriptor save = MethodDescriptor.ofMethod(generatedClassName, "save", entityTypeStr,
                        entityTypeStr);

                try (MethodCreator saveAll = classCreator.getMethodCreator(saveAllDescriptor)) {
                    saveAll.setSignature(String.format("<S:L%s;>(Ljava/lang/Iterable<TS;>;)Ljava/util/List<TS;>;",
                            entityTypeStr.replace('.', '/')));
                    saveAll.addAnnotation(Transactional.class);

                    ResultHandle iterable = saveAll.getMethodParam(0);
                    ResultHandle resultList = saveAll.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));

                    ResultHandle iterator = saveAll.invokeInterfaceMethod(
                            ofMethod(Iterable.class, "iterator", Iterator.class),
                            iterable);
                    BytecodeCreator loop = saveAll.createScope();
                    ResultHandle hasNextValue = loop.invokeInterfaceMethod(
                            ofMethod(Iterator.class, "hasNext", boolean.class),
                            iterator);

                    BranchResult hasNextBranch = loop.ifNonZero(hasNextValue);
                    BytecodeCreator hasNext = hasNextBranch.trueBranch();
                    BytecodeCreator doesNotHaveNext = hasNextBranch.falseBranch();
                    ResultHandle next = hasNext.invokeInterfaceMethod(
                            ofMethod(Iterator.class, "next", Object.class),
                            iterator);
                    ResultHandle saveResult = hasNext.invokeVirtualMethod(save, hasNext.getThis(), next);
                    hasNext.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                            resultList, saveResult);
                    hasNext.continueScope(loop);

                    doesNotHaveNext.breakScope(loop);

                    saveAll.returnValue(resultList);
                }
                try (MethodCreator bridgeSaveAll = classCreator.getMethodCreator(bridgeSaveAllDescriptor)) {
                    MethodDescriptor saveAll = MethodDescriptor.ofMethod(generatedClassName, "saveAll",
                            List.class.getName(), Iterable.class);
                    ResultHandle result = bridgeSaveAll.invokeVirtualMethod(saveAll, bridgeSaveAll.getThis(),
                            bridgeSaveAll.getMethodParam(0));
                    bridgeSaveAll.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(saveAllDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveAllDescriptor, true);
        }
    }

    private void generateFlush(ClassCreator classCreator, String generatedClassName,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor flushDescriptor = MethodDescriptor.ofMethod(generatedClassName, "flush", void.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(flushDescriptor)) {

            if (!classCreator.getExistingMethods().contains(flushDescriptor)) {
                try (MethodCreator flush = classCreator.getMethodCreator(flushDescriptor)) {
                    flush.addAnnotation(Transactional.class);
                    flush.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "flush", void.class),
                            flush.readStaticField(operationsField));
                    flush.returnValue(null);
                }
            }

            allMethodsToBeImplementedToResult.put(flushDescriptor, true);
        }
    }

    private void generateFindById(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor findByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findById",
                Optional.class.getName(), idTypeStr);
        MethodDescriptor bridgeFindByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findById",
                Optional.class.getName(), Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(findByIdDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindByIdDescriptor)) {

            if (!classCreator.getExistingMethods().contains(findByIdDescriptor)) {
                try (MethodCreator findById = classCreator.getMethodCreator(findByIdDescriptor)) {
                    findById.setSignature(String.format("(L%s;)Ljava/util/Optional<L%s;>;",
                            idTypeStr.replace('.', '/'), entityTypeStr.replace('.', '/')));
                    ResultHandle id = findById.getMethodParam(0);
                    ResultHandle entity = findById.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "findById", Object.class, Class.class,
                                    Object.class),
                            findById.readStaticField(operationsField),
                            findById.readInstanceField(entityClassFieldDescriptor, findById.getThis()), id);
                    ResultHandle optional = findById.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Optional.class, "ofNullable", Optional.class, Object.class),
                            entity);
                    findById.returnValue(optional);
                }
                try (MethodCreator bridgeFindById = classCreator.getMethodCreator(bridgeFindByIdDescriptor)) {
                    MethodDescriptor findById = MethodDescriptor.ofMethod(generatedClassName, "findById",
                            Optional.class.getName(),
                            idTypeStr);
                    ResultHandle methodParam = bridgeFindById.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeFindById.checkCast(methodParam, idTypeStr);
                    ResultHandle result = bridgeFindById.invokeVirtualMethod(findById, bridgeFindById.getThis(),
                            castedMethodParam);
                    bridgeFindById.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(findByIdDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeFindByIdDescriptor, true);
        }
    }

    private void generateExistsById(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor existsByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "existsById",
                boolean.class, idTypeStr);
        MethodDescriptor bridgeExistsByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "existsById", boolean.class,
                Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(existsByIdDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeExistsByIdDescriptor)) {

            if (!classCreator.getExistingMethods().contains(existsByIdDescriptor)) {
                MethodDescriptor findById = MethodDescriptor.ofMethod(generatedClassName, "findById",
                        Optional.class.getName(),
                        idTypeStr);

                // we need to force the generation of findById since this method depends on it
                allMethodsToBeImplementedToResult.put(findById, false);
                generateFindById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                        allMethodsToBeImplementedToResult);

                try (MethodCreator existsById = classCreator.getMethodCreator(existsByIdDescriptor)) {

                    ResultHandle methodParam = existsById.getMethodParam(0);
                    ResultHandle optional = existsById.invokeVirtualMethod(findById, existsById.getThis(),
                            methodParam);
                    ResultHandle isPresent = existsById.invokeVirtualMethod(
                            ofMethod(Optional.class, "isPresent", boolean.class),
                            optional);
                    existsById.returnValue(isPresent);
                }
                try (MethodCreator bridgeExistsById = classCreator.getMethodCreator(bridgeExistsByIdDescriptor)) {
                    MethodDescriptor existsById = MethodDescriptor.ofMethod(generatedClassName, "existsById",
                            boolean.class.getName(),
                            idTypeStr);
                    ResultHandle methodParam = bridgeExistsById.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeExistsById.checkCast(methodParam, idTypeStr);
                    ResultHandle result = bridgeExistsById.invokeVirtualMethod(existsById, bridgeExistsById.getThis(),
                            castedMethodParam);
                    bridgeExistsById.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(existsByIdDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeExistsByIdDescriptor, true);
        }
    }

    private void generateGetOne(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor getOneDescriptor = MethodDescriptor.ofMethod(generatedClassName, "getOne",
                entityTypeStr, idTypeStr);
        MethodDescriptor bridgeGetOneDescriptor = MethodDescriptor.ofMethod(generatedClassName, "getOne",
                Object.class, Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(getOneDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeGetOneDescriptor)) {

            if (!classCreator.getExistingMethods().contains(getOneDescriptor)) {
                try (MethodCreator findById = classCreator.getMethodCreator(getOneDescriptor)) {

                    ResultHandle entity = findById.invokeStaticMethod(ofMethod(RepositorySupport.class, "getOne",
                            Object.class, AbstractJpaOperations.class, Class.class, Object.class),
                            findById.readStaticField(operationsField),
                            findById.readInstanceField(entityClassFieldDescriptor, findById.getThis()),
                            findById.getMethodParam(0));

                    findById.returnValue(entity);
                }
                try (MethodCreator bridgeGetOne = classCreator.getMethodCreator(bridgeGetOneDescriptor)) {
                    MethodDescriptor getOne = MethodDescriptor.ofMethod(generatedClassName, "getOne",
                            entityTypeStr, idTypeStr);
                    ResultHandle methodParam = bridgeGetOne.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeGetOne.checkCast(methodParam, idTypeStr);
                    ResultHandle result = bridgeGetOne.invokeVirtualMethod(getOne, bridgeGetOne.getThis(),
                            castedMethodParam);
                    bridgeGetOne.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(getOneDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeGetOneDescriptor, true);
        }
    }

    private void generateFindAll(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor findAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAll", List.class);
        MethodDescriptor bridgeFindAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAll", Iterable.class);

        if (allMethodsToBeImplementedToResult.containsKey(findAllDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllDescriptor)) {

            if (!classCreator.getExistingMethods().contains(findAllDescriptor)) {
                try (MethodCreator findAll = classCreator.getMethodCreator(findAllDescriptor)) {
                    findAll.setSignature(String.format("()Ljava/util/List<L%s;>;",
                            entityTypeStr.replace('.', '/')));
                    ResultHandle panacheQuery = findAll.invokeVirtualMethod(
                            ofMethod(AbstractJpaOperations.class, "findAll", Object.class, Class.class),
                            findAll.readStaticField(operationsField),
                            findAll.readInstanceField(entityClassFieldDescriptor, findAll.getThis()));
                    ResultHandle list = findAll.invokeInterfaceMethod(
                            ofMethod(PanacheQuery.class, "list", List.class),
                            panacheQuery);
                    findAll.returnValue(list);
                }
                try (MethodCreator bridgeFindAll = classCreator.getMethodCreator(bridgeFindAllDescriptor)) {
                    MethodDescriptor findAll = MethodDescriptor.ofMethod(generatedClassName, "findAll", List.class.getName());
                    ResultHandle result = bridgeFindAll.invokeVirtualMethod(findAll, bridgeFindAll.getThis());
                    bridgeFindAll.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(findAllDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllDescriptor, true);
        }
    }

    private void generateFindAllWithSort(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor findAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAll", List.class, Sort.class);
        MethodDescriptor bridgeFindAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAll", Iterable.class,
                Sort.class);

        if (allMethodsToBeImplementedToResult.containsKey(findAllDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllDescriptor)) {

            if (!classCreator.getExistingMethods().contains(findAllDescriptor)) {
                try (MethodCreator findAll = classCreator.getMethodCreator(findAllDescriptor)) {
                    findAll.setSignature(String.format("(Lorg/springframework/data/domain/Sort;)Ljava/util/List<L%s;>;",
                            entityTypeStr.replace('.', '/')));

                    ResultHandle sort = findAll.invokeStaticMethod(
                            MethodDescriptor.ofMethod(TypesConverter.class, "toPanacheSort",
                                    io.quarkus.panache.common.Sort.class,
                                    org.springframework.data.domain.Sort.class),
                            findAll.getMethodParam(0));

                    ResultHandle panacheQuery = findAll.invokeVirtualMethod(
                            ofMethod(AbstractJpaOperations.class, "findAll", Object.class, Class.class,
                                    io.quarkus.panache.common.Sort.class),
                            findAll.readStaticField(operationsField),
                            findAll.readInstanceField(entityClassFieldDescriptor, findAll.getThis()), sort);
                    ResultHandle list = findAll.invokeInterfaceMethod(
                            ofMethod(PanacheQuery.class, "list", List.class),
                            panacheQuery);
                    findAll.returnValue(list);
                }
                try (MethodCreator bridgeFindAll = classCreator.getMethodCreator(bridgeFindAllDescriptor)) {
                    MethodDescriptor findAll = MethodDescriptor.ofMethod(generatedClassName, "findAll", List.class.getName(),
                            Sort.class);
                    ResultHandle result = bridgeFindAll.invokeVirtualMethod(findAll, bridgeFindAll.getThis(),
                            bridgeFindAll.getMethodParam(0));
                    bridgeFindAll.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(findAllDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllDescriptor, true);
        }
    }

    private void generateFindAllWithPageable(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor findAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAll", Page.class,
                Pageable.class);

        if (allMethodsToBeImplementedToResult.containsKey(findAllDescriptor)) {

            if (!classCreator.getExistingMethods().contains(findAllDescriptor)) {
                try (MethodCreator findAll = classCreator.getMethodCreator(findAllDescriptor)) {
                    findAll.setSignature(String.format(
                            "(Lorg/springframework/data/domain/Pageable;)Lorg/springframework/data/domain/Page<L%s;>;",
                            entityTypeStr.replace('.', '/')));

                    ResultHandle pageable = findAll.getMethodParam(0);
                    ResultHandle pageableSort = findAll.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Pageable.class, "getSort", Sort.class),
                            pageable);

                    ResultHandle panachePage = findAll.invokeStaticMethod(
                            MethodDescriptor.ofMethod(TypesConverter.class, "toPanachePage",
                                    io.quarkus.panache.common.Page.class, Pageable.class),
                            pageable);
                    ResultHandle panacheSort = findAll.invokeStaticMethod(
                            MethodDescriptor.ofMethod(TypesConverter.class, "toPanacheSort",
                                    io.quarkus.panache.common.Sort.class,
                                    org.springframework.data.domain.Sort.class),
                            pageableSort);

                    // depending on whether there was a io.quarkus.panache.common.Sort returned, we need to execute a different findAll method
                    BranchResult sortNullBranch = findAll.ifNull(panacheSort);
                    BytecodeCreator sortNullTrue = sortNullBranch.trueBranch();
                    BytecodeCreator sortNullFalse = sortNullBranch.falseBranch();
                    AssignableResultHandle panacheQueryVar = findAll.createVariable(PanacheQuery.class);

                    ResultHandle panacheQueryWithoutSort = sortNullTrue.invokeVirtualMethod(
                            ofMethod(AbstractJpaOperations.class, "findAll", Object.class, Class.class),
                            sortNullTrue.readStaticField(operationsField),
                            sortNullTrue.readInstanceField(entityClassFieldDescriptor, sortNullTrue.getThis()));
                    sortNullTrue.assign(panacheQueryVar, panacheQueryWithoutSort);
                    sortNullTrue.breakScope();

                    ResultHandle panacheQueryWithSort = sortNullFalse.invokeVirtualMethod(
                            ofMethod(AbstractJpaOperations.class, "findAll", Object.class, Class.class,
                                    io.quarkus.panache.common.Sort.class),
                            sortNullFalse.readStaticField(operationsField),
                            sortNullFalse.readInstanceField(entityClassFieldDescriptor, sortNullFalse.getThis()), panacheSort);
                    sortNullFalse.assign(panacheQueryVar, panacheQueryWithSort);
                    sortNullFalse.breakScope();

                    ResultHandle panacheQuery = findAll.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PanacheQuery.class, "page", PanacheQuery.class,
                                    io.quarkus.panache.common.Page.class),
                            panacheQueryVar, panachePage);
                    ResultHandle list = findAll.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PanacheQuery.class, "list", List.class),
                            panacheQuery);
                    ResultHandle count = findAll.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PanacheQuery.class, "count", long.class),
                            panacheQuery);
                    ResultHandle pageResult = findAll.newInstance(
                            MethodDescriptor.ofConstructor(PageImpl.class, List.class, Pageable.class, long.class),
                            list, findAll.getMethodParam(0), count);

                    findAll.returnValue(pageResult);
                }
            }

            allMethodsToBeImplementedToResult.put(findAllDescriptor, true);
        }
    }

    private void generateFindAllById(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, DotName entityDotName, String entityTypeStr, String idTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor findAllByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAllById", List.class,
                Iterable.class);
        MethodDescriptor bridgeFindAllByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "findAllById",
                Iterable.class, Iterable.class);

        if (allMethodsToBeImplementedToResult.containsKey(findAllByIdDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllByIdDescriptor)) {

            if (!classCreator.getExistingMethods().contains(findAllByIdDescriptor)) {
                try (MethodCreator findAllById = classCreator.getMethodCreator(findAllByIdDescriptor)) {
                    findAllById.setSignature(String.format("(Ljava/lang/Iterable<L%s;>;)Ljava/util/List<L%s;>;",
                            idTypeStr.replace('.', '/'), entityTypeStr.replace('.', '/')));

                    ResultHandle entityClass = findAllById.readInstanceField(entityClassFieldDescriptor,
                            findAllById.getThis());

                    ResultHandle list;
                    AnnotationTarget idAnnotationTarget = getIdAnnotationTarget(entityDotName, index);
                    FieldInfo idField = getIdField(idAnnotationTarget);
                    if ((idField != null) &&
                            (DotNames.LONG.equals(idField.type().name()) || DotNames.INTEGER.equals(idField.type().name())
                                    || DotNames.STRING.equals(idField.type().name()))) {
                        MethodDescriptor method = ofMethod(RepositorySupport.class, "findByIds",
                                List.class, AbstractJpaOperations.class, Class.class, String.class,
                                Iterable.class);
                        list = findAllById.invokeStaticMethod(method,
                                findAllById.readStaticField(operationsField),
                                entityClass,
                                findAllById.load(idField.name()), findAllById.getMethodParam(0));
                    } else {
                        list = findAllById.invokeStaticMethod(
                                MethodDescriptor.ofMethod(RepositorySupport.class, "findByIds",
                                        List.class, AbstractJpaOperations.class, Class.class, Iterable.class),
                                findAllById.readStaticField(operationsField),
                                entityClass,
                                findAllById.getMethodParam(0));
                    }

                    findAllById.returnValue(list);
                }
                try (MethodCreator bridgeFindAllById = classCreator.getMethodCreator(bridgeFindAllByIdDescriptor)) {
                    MethodDescriptor findAllById = MethodDescriptor.ofMethod(generatedClassName, "findAllById",
                            List.class.getName(), Iterable.class);
                    ResultHandle result = bridgeFindAllById.invokeVirtualMethod(findAllById, bridgeFindAllById.getThis(),
                            bridgeFindAllById.getMethodParam(0));
                    bridgeFindAllById.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(findAllByIdDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllByIdDescriptor, true);
        }
    }

    private FieldInfo getIdField(AnnotationTarget idAnnotationTarget) {
        if (idAnnotationTarget instanceof FieldInfo) {
            return idAnnotationTarget.asField();
        }

        MethodInfo methodInfo = idAnnotationTarget.asMethod();
        String propertyName = JavaBeanUtil.getPropertyNameFromGetter(methodInfo.name());
        ClassInfo entityClass = methodInfo.declaringClass();
        FieldInfo field = entityClass.field(propertyName);
        if (field == null) {
            throw new IllegalArgumentException("Entity " + entityClass + " does not appear to have a field backing method"
                    + methodInfo.name() + " which is annotated with @Id");
        }
        return field;
    }

    private void generateCount(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor, String generatedClassName,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor countDescriptor = MethodDescriptor.ofMethod(generatedClassName, "count", long.class);

        if (allMethodsToBeImplementedToResult.containsKey(countDescriptor)) {
            if (!classCreator.getExistingMethods().contains(countDescriptor)) {
                try (MethodCreator count = classCreator.getMethodCreator(countDescriptor)) {
                    ResultHandle result = count.invokeVirtualMethod(
                            ofMethod(AbstractJpaOperations.class, "count", long.class, Class.class),
                            count.readStaticField(operationsField),
                            count.readInstanceField(entityClassFieldDescriptor, count.getThis()));
                    count.returnValue(result);
                }
            }
            allMethodsToBeImplementedToResult.put(countDescriptor, true);
        }
    }

    private void generateDeleteById(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor deleteByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "deleteById",
                void.class.getName(), idTypeStr);
        MethodDescriptor bridgeDeleteByIdDescriptor = MethodDescriptor.ofMethod(generatedClassName, "deleteById",
                void.class, Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(deleteByIdDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeDeleteByIdDescriptor)) {

            if (!classCreator.getExistingMethods().contains(deleteByIdDescriptor)) {
                try (MethodCreator deleteById = classCreator.getMethodCreator(deleteByIdDescriptor)) {
                    deleteById.addAnnotation(Transactional.class);
                    ResultHandle id = deleteById.getMethodParam(0);
                    ResultHandle entityClass = deleteById.readInstanceField(entityClassFieldDescriptor,
                            deleteById.getThis());

                    ResultHandle deleted = deleteById.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "deleteById", boolean.class,
                                    Class.class, Object.class),
                            deleteById.readStaticField(operationsField), entityClass, id);

                    BranchResult deletedBranch = deleteById.ifNonZero(deleted);
                    BytecodeCreator deletedFalse = deletedBranch.falseBranch();

                    ResultHandle idToString = deletedFalse.invokeVirtualMethod(
                            ofMethod(Object.class, "toString", String.class),
                            id);
                    ResultHandle formatArgsArray = deletedFalse.newArray(Object.class, 1);
                    deletedFalse.writeArrayValue(formatArgsArray, deletedFalse.load(0), idToString);

                    ResultHandle messageFormat = deletedFalse.load("No entity " + entityTypeStr + " with id %s exists");
                    ResultHandle message = deletedFalse.invokeStaticMethod(
                            MethodDescriptor.ofMethod(String.class, "format", String.class, String.class, Object[].class),
                            messageFormat, formatArgsArray);

                    ResultHandle exception = deletedFalse.newInstance(
                            MethodDescriptor.ofConstructor(IllegalArgumentException.class, String.class),
                            message);
                    deletedFalse.throwException(exception);
                    deletedFalse.breakScope();

                    deleteById.returnValue(null);
                }
                try (MethodCreator bridgeDeleteById = classCreator.getMethodCreator(bridgeDeleteByIdDescriptor)) {
                    MethodDescriptor deleteById = MethodDescriptor.ofMethod(generatedClassName, "deleteById",
                            void.class, idTypeStr);
                    ResultHandle methodParam = bridgeDeleteById.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeDeleteById.checkCast(methodParam, idTypeStr);
                    ResultHandle result = bridgeDeleteById.invokeVirtualMethod(deleteById, bridgeDeleteById.getThis(),
                            castedMethodParam);
                    bridgeDeleteById.returnValue(result);
                }
            }

            allMethodsToBeImplementedToResult.put(deleteByIdDescriptor, true);
            allMethodsToBeImplementedToResult.put(bridgeDeleteByIdDescriptor, true);
        }
    }

    private void generateDelete(ClassCreator classCreator, String generatedClassName, String entityTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor deleteDescriptor = MethodDescriptor.ofMethod(generatedClassName, "delete",
                void.class.toString(), entityTypeStr);
        MethodDescriptor bridgeDeleteDescriptor = MethodDescriptor.ofMethod(generatedClassName, "delete", void.class,
                Object.class);

        if (allMethodsToBeImplementedToResult.containsKey(deleteDescriptor)
                || allMethodsToBeImplementedToResult.containsKey(bridgeDeleteDescriptor)) {

            if (!classCreator.getExistingMethods().contains(deleteDescriptor)) {
                try (MethodCreator delete = classCreator.getMethodCreator(deleteDescriptor)) {
                    delete.addAnnotation(Transactional.class);
                    ResultHandle entity = delete.getMethodParam(0);
                    delete.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "delete", void.class, Object.class),
                            delete.readStaticField(operationsField), entity);
                    delete.returnValue(null);
                }
                try (MethodCreator bridgeDelete = classCreator.getMethodCreator(bridgeDeleteDescriptor)) {
                    MethodDescriptor delete = MethodDescriptor.ofMethod(generatedClassName, "delete", void.class.toString(),
                            entityTypeStr);
                    ResultHandle methodParam = bridgeDelete.getMethodParam(0);
                    ResultHandle castedMethodParam = bridgeDelete.checkCast(methodParam, entityTypeStr);
                    ResultHandle result = bridgeDelete.invokeVirtualMethod(delete, bridgeDelete.getThis(),
                            castedMethodParam);
                    bridgeDelete.returnValue(result);
                }
            }
        }

        allMethodsToBeImplementedToResult.put(deleteDescriptor, true);
        allMethodsToBeImplementedToResult.put(bridgeDeleteDescriptor, true);
    }

    private void generateDeleteAllWithIterable(ClassCreator classCreator, String generatedClassName, String entityTypeStr,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor deleteAllWithIterableDescriptor = MethodDescriptor.ofMethod(generatedClassName, "deleteAll",
                void.class, Iterable.class);

        if (allMethodsToBeImplementedToResult.containsKey(deleteAllWithIterableDescriptor)) {
            if (!classCreator.getExistingMethods().contains(deleteAllWithIterableDescriptor)) {
                try (MethodCreator deleteAll = classCreator.getMethodCreator(deleteAllWithIterableDescriptor)) {
                    deleteAll.setSignature(String.format("(Ljava/lang/Iterable<+L%s;>;)V",
                            entityTypeStr.replace('.', '/')));
                    deleteAll.addAnnotation(Transactional.class);
                    ResultHandle entities = deleteAll.getMethodParam(0);
                    deleteAll.invokeStaticMethod(
                            MethodDescriptor.ofMethod(RepositorySupport.class, "deleteAll",
                                    void.class, AbstractJpaOperations.class, Iterable.class),
                            deleteAll.readStaticField(operationsField),
                            entities);
                    deleteAll.returnValue(null);
                }
            }
            allMethodsToBeImplementedToResult.put(deleteAllWithIterableDescriptor, true);
        }
    }

    private void generateDeleteAll(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor deleteAllDescriptor = MethodDescriptor.ofMethod(generatedClassName, "deleteAll", void.class);

        if (allMethodsToBeImplementedToResult.containsKey(deleteAllDescriptor)) {
            if (!classCreator.getExistingMethods().contains(deleteAllDescriptor)) {
                try (MethodCreator deleteAll = classCreator.getMethodCreator(deleteAllDescriptor)) {
                    deleteAll.addAnnotation(Transactional.class);
                    deleteAll.invokeStaticMethod(
                            MethodDescriptor.ofMethod(AdditionalJpaOperations.class, "deleteAllWithCascade", long.class,
                                    AbstractJpaOperations.class, Class.class.getName()),
                            deleteAll.readStaticField(operationsField),
                            deleteAll.readInstanceField(entityClassFieldDescriptor, deleteAll.getThis()));
                    deleteAll.returnValue(null);
                }
            }
            allMethodsToBeImplementedToResult.put(deleteAllDescriptor, true);
        }
    }

    private void generateDeleteAllInBatch(ClassCreator classCreator, FieldDescriptor entityClassFieldDescriptor,
            String generatedClassName, Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {

        MethodDescriptor deleteAllInBatchDescriptor = MethodDescriptor.ofMethod(generatedClassName, "deleteAllInBatch",
                void.class);

        if (allMethodsToBeImplementedToResult.containsKey(deleteAllInBatchDescriptor)) {
            if (!classCreator.getExistingMethods().contains(deleteAllInBatchDescriptor)) {
                try (MethodCreator deleteAll = classCreator.getMethodCreator(deleteAllInBatchDescriptor)) {
                    deleteAll.addAnnotation(Transactional.class);
                    ResultHandle result = deleteAll.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractJpaOperations.class, "deleteAll", long.class, Class.class),
                            deleteAll.readStaticField(operationsField),
                            deleteAll.readInstanceField(entityClassFieldDescriptor, deleteAll.getThis()));
                    deleteAll.returnValue(result);
                }
            }
            allMethodsToBeImplementedToResult.put(deleteAllInBatchDescriptor, true);
        }
    }

    private void handleUnimplementedMethods(ClassCreator classCreator,
            Map<MethodDescriptor, Boolean> allMethodsToBeImplementedToResult) {
        for (Map.Entry<MethodDescriptor, Boolean> entry : allMethodsToBeImplementedToResult.entrySet()) {
            if (entry.getValue()) { // ignore implemented methods
                continue;
            }

            try (MethodCreator methodCreator = classCreator.getMethodCreator(entry.getKey())) {
                ResultHandle res = methodCreator.newInstance(
                        MethodDescriptor.ofConstructor(FunctionalityNotImplemented.class, String.class, String.class),
                        methodCreator.load(classCreator.getClassName().replace('/', '.')),
                        methodCreator.load(entry.getKey().getName()));
                methodCreator.throwException(res);
            }
        }
    }

    private Set<MethodInfo> methodsOfExtendedSpringDataRepositories(ClassInfo repositoryToImplement) {
        return GenerationUtil.interfaceMethods(GenerationUtil.extendedSpringDataRepos(repositoryToImplement, index), index);
    }

    // Spring Data allows users to add any of the methods of CrudRepository, PagingAndSortingRepository, JpaRepository
    // to the their interface declaration without having to make their repository extend any of those
    // this is done so users have the ability to add only what they need
    private Set<MethodInfo> stockMethodsAddedToInterface(ClassInfo repositoryToImplement) {
        Set<MethodInfo> result = new HashSet<>();

        Set<MethodInfo> allSpringDataRepositoryMethods = allSpringDataRepositoryMethods();
        for (MethodInfo method : repositoryToImplement.methods()) {
            for (MethodInfo springDataRepositoryMethod : allSpringDataRepositoryMethods) {
                if (canMethodsBeConsideredSame(method, springDataRepositoryMethod)) {
                    result.add(method);
                }
            }
        }

        return result;
    }

    private Set<MethodInfo> allSpringDataRepositoryMethods() {
        if (ALL_SPRING_DATA_REPOSITORY_METHODS != null) {
            return ALL_SPRING_DATA_REPOSITORY_METHODS;
        }

        ALL_SPRING_DATA_REPOSITORY_METHODS = GenerationUtil.interfaceMethods(new HashSet<>(DotNames.SUPPORTED_REPOSITORIES),
                index);

        return ALL_SPRING_DATA_REPOSITORY_METHODS;
    }

    // Used to determine if a method with captured generic types can be considered the same as a target method
    // This is rather naive but works in the constraints of Spring Data
    private boolean canMethodsBeConsideredSame(MethodInfo candidate, MethodInfo target) {
        if (!candidate.name().equals(target.name())) {
            return false;
        }

        if (candidate.parameters().size() != target.parameters().size()) {
            return false;
        }

        if (!canTypesBeConsideredSame(candidate.returnType(), target.returnType())) {
            return false;
        }

        for (int i = 0; i < candidate.parameters().size(); i++) {
            if (!canTypesBeConsideredSame(candidate.parameters().get(i), target.parameters().get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean canTypesBeConsideredSame(Type candidate, Type target) {
        if (candidate.equals(target)) {
            return true;
        }

        if ((candidate instanceof ParameterizedType) && target instanceof ParameterizedType) {
            return candidate.asParameterizedType().name().equals(target.asParameterizedType().name());
        }

        return (candidate instanceof ClassType) && (target instanceof TypeVariable);
    }

    private AnnotationTarget getIdAnnotationTarget(DotName entityDotName, IndexView index) {
        return getIdAnnotationTargetRec(entityDotName, index, entityDotName);
    }

    private AnnotationTarget getIdAnnotationTargetRec(DotName currentDotName, IndexView index, DotName originalEntityDotName) {
        ClassInfo classInfo = index.getClassByName(currentDotName);
        if (classInfo == null) {
            throw new IllegalStateException("Entity " + originalEntityDotName + " was not part of the Quarkus index");
        }

        if (!classInfo.annotations().containsKey(DotNames.JPA_ID)) {
            if (DotNames.OBJECT.equals(classInfo.superName())) {
                throw new IllegalArgumentException("Currently only Entities with the @Id annotation are supported. " +
                        "Offending class is " + originalEntityDotName);
            }
            return getIdAnnotationTargetRec(classInfo.superName(), index, originalEntityDotName);
        }

        List<AnnotationInstance> annotationInstances = classInfo.annotations().get(DotNames.JPA_ID);
        if (annotationInstances.size() > 1) {
            throw new IllegalArgumentException(
                    "Currently the @Id annotation can only be placed on a single field or method. " +
                            "Offending class is " + originalEntityDotName);
        }

        return annotationInstances.get(0).target();
    }

    private Optional<AnnotationTarget> getVersionAnnotationTarget(DotName entityDotName, IndexView index) {
        return getVersionAnnotationTargetRec(entityDotName, index, entityDotName);
    }

    private Optional<AnnotationTarget> getVersionAnnotationTargetRec(DotName currentDotName, IndexView index,
            DotName originalEntityDotName) {
        ClassInfo classInfo = index.getClassByName(currentDotName);
        if (classInfo == null) {
            throw new IllegalStateException("Entity " + originalEntityDotName + " was not part of the Quarkus index");
        }

        if (!classInfo.annotations().containsKey(DotNames.VERSION)) {
            if (DotNames.OBJECT.equals(classInfo.superName())) {
                return Optional.empty();
            }
            return getVersionAnnotationTargetRec(classInfo.superName(), index, originalEntityDotName);
        }

        List<AnnotationInstance> annotationInstances = classInfo.annotations().get(DotNames.VERSION);
        if (annotationInstances.size() > 1) {
            throw new IllegalArgumentException(
                    "Currently the @Version annotation can only be placed on a single field or method. " +
                            "Offending class is " + originalEntityDotName);
        }

        return Optional.of(annotationInstances.get(0).target());
    }
}
