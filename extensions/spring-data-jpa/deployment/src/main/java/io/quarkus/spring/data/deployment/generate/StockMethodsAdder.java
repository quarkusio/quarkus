package io.quarkus.spring.data.deployment.generate;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.hibernate.Session;
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
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.hibernate.orm.panache.runtime.AdditionalJpaOperations;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.runtime.FunctionalityNotImplemented;
import io.quarkus.spring.data.runtime.RepositorySupport;
import io.quarkus.spring.data.runtime.TypesConverter;

public class StockMethodsAdder {

    private static Set<MethodInfo> ALL_SPRING_DATA_REPOSITORY_METHODS = null;

    private final IndexView index;
    private final FieldDesc operationsField;

    public StockMethodsAdder(IndexView index, TypeBundle typeBundle) {
        this.index = index;
        String operationsName = typeBundle.operations().dotName().toString();
        operationsField = FieldDesc.of(ClassDesc.of(operationsName), "INSTANCE", ClassDesc.of(operationsName));
    }

    public void add(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, ClassInfo repositoryToImplement, DotName entityDotName, String idTypeStr,
            Set<String> existingMethods) {

        Set<MethodInfo> methodsOfExtendedSpringDataRepositories = methodsOfExtendedSpringDataRepositories(
                repositoryToImplement);
        Set<MethodInfo> stockMethodsAddedToInterface = stockMethodsAddedToInterface(repositoryToImplement);
        Set<MethodInfo> allMethodsToBeImplemented = new LinkedHashSet<>(methodsOfExtendedSpringDataRepositories);
        allMethodsToBeImplemented.addAll(stockMethodsAddedToInterface);

        Map<String, Boolean> allMethodsToBeImplementedToResult = new LinkedHashMap<>();
        for (MethodInfo methodInfo : allMethodsToBeImplemented) {
            allMethodsToBeImplementedToResult.put(GenerationUtil.methodKey(generatedClassName, methodInfo), false);
        }

        String entityTypeStr = entityDotName.toString();

        generateSave(classCreator, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult, entityClassFieldDescriptor, existingMethods);
        generateSaveAndFlush(classCreator, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult, entityClassFieldDescriptor, existingMethods);
        generateSaveAll(classCreator, entityClassFieldDescriptor, generatedClassName, entityDotName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateFlush(classCreator, generatedClassName, allMethodsToBeImplementedToResult, existingMethods);
        generateFindById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateExistsById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateGetOne(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateGetReferenceById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateGetById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateFindAll(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateFindAllWithSort(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateFindAllWithPageable(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateFindAllById(classCreator, entityClassFieldDescriptor, generatedClassName, entityDotName, entityTypeStr,
                idTypeStr, allMethodsToBeImplementedToResult, existingMethods);
        generateCount(classCreator, entityClassFieldDescriptor, generatedClassName, allMethodsToBeImplementedToResult,
                existingMethods);
        generateDeleteById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateDelete(classCreator, generatedClassName, entityTypeStr, allMethodsToBeImplementedToResult, existingMethods);
        generateDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, allMethodsToBeImplementedToResult,
                existingMethods);
        generateDeleteAll(classCreator, entityClassFieldDescriptor, generatedClassName, allMethodsToBeImplementedToResult,
                existingMethods);
        generateDeleteAllInBatch(classCreator, entityClassFieldDescriptor, generatedClassName,
                allMethodsToBeImplementedToResult, existingMethods);
        generateDeleteAllInBatchWithIterable(classCreator, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateDeleteInBatchWithIterable(classCreator, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);
        generateDeleteAllByIdInBatchWithIterable(classCreator, generatedClassName, entityTypeStr,
                allMethodsToBeImplementedToResult, existingMethods);

        handleUnimplementedMethods(classCreator, generatedClassName, allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateSave(ClassCreator classCreator, String generatedClassName,
            DotName entityDotName, String entityTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult,
            FieldDesc entityClassFieldDescriptor, Set<String> existingMethods) {

        String saveKey = GenerationUtil.methodKey("save", entityTypeStr, entityTypeStr);
        String bridgeSaveKey = GenerationUtil.methodKey("save", Object.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(saveKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveKey)) {

            if (!existingMethods.contains(saveKey)) {
                MethodTypeDesc saveMtd = GenerationUtil.toMethodTypeDesc(entityTypeStr, entityTypeStr);
                classCreator.method("save", mc -> {
                    mc.setType(saveMtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar entityParam = mc.parameter("entity");

                    mc.body(bc -> {
                        // Read the static operations field and entity class field once and store in LocalVars
                        // so they can be used across ifElse branches
                        LocalVar opsVar = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClassVar = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        if (isPersistable(entityDotName)) {
                            Expr isNew = bc.invokeVirtual(
                                    ClassMethodDesc.of(ClassDesc.of(entityDotName.toString()), "isNew",
                                            MethodTypeDesc.of(ConstantDescs.CD_boolean)),
                                    entityParam);
                            bc.ifElse(isNew,
                                    tb -> generatePersistAndReturn(entityParam, tb, opsVar),
                                    fb -> generateMergeAndReturn(entityParam, fb, opsVar,
                                            entityClassVar));
                        } else {
                            AnnotationTarget idAnnotationTarget = getIdAnnotationTarget(entityDotName, index);
                            Expr idValue = generateObtainValue(bc, entityDotName, entityParam, idAnnotationTarget);
                            Type idType = getTypeOfTarget(idAnnotationTarget);
                            Optional<AnnotationTarget> versionValueTarget = getVersionAnnotationTarget(entityDotName, index);

                            if (versionValueTarget.isPresent()) {
                                Type versionType = getTypeOfTarget(versionValueTarget.get());
                                if (versionType instanceof PrimitiveType) {
                                    throw new IllegalArgumentException(
                                            "The '@Version' annotation cannot be used on primitive types. Offending entity is '"
                                                    + entityDotName + "'.");
                                }
                                Expr versionValue = generateObtainValue(bc, entityDotName, entityParam,
                                        versionValueTarget.get());
                                bc.ifElse(bc.isNull(versionValue),
                                        tb -> generatePersistAndReturn(entityParam, tb, opsVar),
                                        fb -> generateMergeAndReturn(entityParam, fb, opsVar,
                                                entityClassVar));
                                // if version is present, we've handled both branches, so return here
                                return;
                            }

                            if (idType instanceof PrimitiveType) {
                                if (!idType.name().equals(DotNames.PRIMITIVE_LONG)
                                        && !idType.name().equals(DotNames.PRIMITIVE_INTEGER)) {
                                    throw new IllegalArgumentException(
                                            "Id type of '" + entityDotName + "' is invalid.");
                                }
                                Expr idValueForComparison = idValue;
                                if (idType.name().equals(DotNames.PRIMITIVE_LONG)) {
                                    Expr longObject = bc.invokeStatic(
                                            MethodDesc.of(Long.class, "valueOf", Long.class, long.class), idValue);
                                    idValueForComparison = bc.invokeVirtual(
                                            MethodDesc.of(Long.class, "intValue", int.class), longObject);
                                }
                                // ifNonZero equivalent: if id != 0 => idValueSet, if id == 0 => idValueUnset
                                bc.ifElse(bc.ne(idValueForComparison, 0),
                                        idValueSetBlock -> generateMergeAndReturn(entityParam, idValueSetBlock,
                                                opsVar, entityClassVar),
                                        idValueUnsetBlock -> generatePersistAndReturn(entityParam,
                                                idValueUnsetBlock, opsVar));
                            } else {
                                bc.ifElse(bc.isNull(idValue),
                                        idValueUnsetBlock -> generatePersistAndReturn(entityParam,
                                                idValueUnsetBlock, opsVar),
                                        idValueSetBlock -> generateMergeAndReturn(entityParam, idValueSetBlock,
                                                opsVar, entityClassVar));
                            }
                        }
                    });
                });
                existingMethods.add(saveKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Object.class.getName(), Object.class.getName());
                MethodDesc saveDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "save", saveMtd);
                classCreator.method("save", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("entity");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(entityTypeStr));
                        Expr result = bbc.invokeVirtual(saveDesc, bmc.this_(), castedParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeSaveKey);
            }

            allMethodsToBeImplementedToResult.put(saveKey, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveKey, true);
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

    private void generatePersistAndReturn(Expr entity, BlockCreator bc, Expr opsExpr) {
        bc.invokeVirtual(
                MethodDesc.of(AbstractManagedJpaOperations.class, "persist", void.class, Object.class),
                opsExpr,
                entity);
        bc.return_(entity);
    }

    private void generateMergeAndReturn(Expr entity, BlockCreator bc, Expr opsExpr,
            Expr entityClassExpr) {
        Expr session = bc.invokeVirtual(
                MethodDesc.of(AbstractManagedJpaOperations.class, "getSession", Session.class, Class.class),
                opsExpr,
                entityClassExpr);
        Expr merged = bc.invokeInterface(
                MethodDesc.of(Session.class, "merge", Object.class, Object.class),
                session, entity);
        bc.return_(merged);
    }

    private Expr generateObtainValue(BlockCreator bc, DotName entityDotName, Expr entity,
            AnnotationTarget annotationTarget) {
        if (annotationTarget instanceof FieldInfo) {
            FieldInfo fieldInfo = annotationTarget.asField();
            if (java.lang.reflect.Modifier.isPublic(fieldInfo.flags())) {
                FieldDesc fd = FieldDesc.of(ClassDesc.of(fieldInfo.declaringClass().name().toString()),
                        fieldInfo.name(), ClassDesc.of(fieldInfo.type().name().toString()));
                return bc.get(entity.field(fd));
            }

            String getterMethodName = JavaBeanUtil.getGetterName(fieldInfo.name(), fieldInfo.type().name());
            return bc.invokeVirtual(
                    ClassMethodDesc.of(ClassDesc.of(entityDotName.toString()), getterMethodName,
                            MethodTypeDesc.of(GenerationUtil.toClassDesc(fieldInfo.type().name().toString()))),
                    entity);
        }
        MethodInfo methodInfo = annotationTarget.asMethod();
        return bc.invokeVirtual(
                ClassMethodDesc.of(ClassDesc.of(entityDotName.toString()), methodInfo.name(),
                        MethodTypeDesc.of(GenerationUtil.toClassDesc(methodInfo.returnType().name().toString()))),
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
            Map<String, Boolean> allMethodsToBeImplementedToResult, FieldDesc entityClassFieldDescriptor,
            Set<String> existingMethods) {

        String saveAndFlushKey = GenerationUtil.methodKey("saveAndFlush", entityTypeStr, entityTypeStr);
        String bridgeSaveAndFlushKey = GenerationUtil.methodKey("saveAndFlush", Object.class.getName(),
                Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(saveAndFlushKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveAndFlushKey)) {

            if (!existingMethods.contains(saveAndFlushKey)) {
                // Force generation of save since this method depends on it
                String saveKey = GenerationUtil.methodKey("save", entityTypeStr, entityTypeStr);
                allMethodsToBeImplementedToResult.put(saveKey, false);
                generateSave(classCreator, generatedClassName, entityDotName, entityTypeStr,
                        allMethodsToBeImplementedToResult, entityClassFieldDescriptor, existingMethods);

                MethodTypeDesc saveMtd = GenerationUtil.toMethodTypeDesc(entityTypeStr, entityTypeStr);
                MethodDesc saveDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "save", saveMtd);

                classCreator.method("saveAndFlush", mc -> {
                    mc.setType(saveMtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar entityParam = mc.parameter("entity");

                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        Expr savedEntity = bc.invokeVirtual(saveDesc, mc.this_(), entityParam);
                        bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "flush", void.class),
                                ops);
                        bc.return_(savedEntity);
                    });
                });
                existingMethods.add(saveAndFlushKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Object.class.getName(), Object.class.getName());
                MethodDesc saveAndFlushDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "saveAndFlush", saveMtd);
                classCreator.method("saveAndFlush", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("entity");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(entityTypeStr));
                        Expr result = bbc.invokeVirtual(saveAndFlushDesc, bmc.this_(), castedParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeSaveAndFlushKey);
            }

            allMethodsToBeImplementedToResult.put(saveAndFlushKey, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveAndFlushKey, true);
        }
    }

    private void generateSaveAll(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, DotName entityDotName, String entityTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String saveAllKey = GenerationUtil.methodKey("saveAll", List.class.getName(), Iterable.class.getName());
        String bridgeSaveAllKey = GenerationUtil.methodKey("saveAll", Iterable.class.getName(), Iterable.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(saveAllKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeSaveAllKey)) {

            if (!existingMethods.contains(saveAllKey)) {
                MethodTypeDesc saveMtd = GenerationUtil.toMethodTypeDesc(entityTypeStr, entityTypeStr);
                MethodDesc saveDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "save", saveMtd);

                MethodTypeDesc saveAllMtd = GenerationUtil.toMethodTypeDesc(List.class.getName(), Iterable.class.getName());
                classCreator.method("saveAll", mc -> {
                    mc.setType(saveAllMtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar iterableParam = mc.parameter("entities");

                    mc.body(bc -> {
                        LocalVar resultList = bc.localVar("resultList",
                                bc.new_(ClassDesc.of(ArrayList.class.getName())));
                        LocalVar iteratorVar = bc.localVar("iterator",
                                bc.invokeInterface(
                                        MethodDesc.of(Iterable.class, "iterator", Iterator.class),
                                        iterableParam));

                        bc.while_(
                                cond -> {
                                    Expr hasNext = cond.invokeInterface(
                                            MethodDesc.of(Iterator.class, "hasNext", boolean.class),
                                            iteratorVar);
                                    cond.yield(hasNext);
                                },
                                body -> {
                                    Expr next = body.invokeInterface(
                                            MethodDesc.of(Iterator.class, "next", Object.class),
                                            iteratorVar);
                                    Expr saveResult = body.invokeVirtual(saveDesc, mc.this_(), next);
                                    body.invokeInterface(
                                            MethodDesc.of(List.class, "add", boolean.class, Object.class),
                                            resultList, saveResult);
                                });

                        bc.return_(resultList);
                    });
                });
                existingMethods.add(saveAllKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Iterable.class.getName(), Iterable.class.getName());
                MethodDesc saveAllDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "saveAll", saveAllMtd);
                classCreator.method("saveAll", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("entities");
                    bmc.body(bbc -> {
                        Expr result = bbc.invokeVirtual(saveAllDesc, bmc.this_(), methodParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeSaveAllKey);
            }

            allMethodsToBeImplementedToResult.put(saveAllKey, true);
            allMethodsToBeImplementedToResult.put(bridgeSaveAllKey, true);
        }
    }

    private void generateFlush(ClassCreator classCreator, String generatedClassName,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String flushKey = GenerationUtil.methodKey("flush", void.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(flushKey)) {
            if (!existingMethods.contains(flushKey)) {
                classCreator.method("flush", mc -> {
                    mc.returning(void.class);
                    mc.addAnnotation(Transactional.class);
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "flush", void.class),
                                ops);
                        bc.return_();
                    });
                });
                existingMethods.add(flushKey);
            }
            allMethodsToBeImplementedToResult.put(flushKey, true);
        }
    }

    private void generateFindById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String findByIdKey = GenerationUtil.methodKey("findById", Optional.class.getName(), idTypeStr);
        String bridgeFindByIdKey = GenerationUtil.methodKey("findById", Optional.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(findByIdKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindByIdKey)) {

            if (!existingMethods.contains(findByIdKey)) {
                MethodTypeDesc findByIdMtd = GenerationUtil.toMethodTypeDesc(Optional.class.getName(), idTypeStr);
                classCreator.method("findById", mc -> {
                    mc.setType(findByIdMtd);
                    ParamVar idParam = mc.parameter("id");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr entity = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "findById", Object.class, Class.class,
                                        Object.class),
                                ops, entityClass, idParam);
                        Expr optional = bc.invokeStatic(
                                MethodDesc.of(Optional.class, "ofNullable", Optional.class, Object.class),
                                entity);
                        bc.return_(optional);
                    });
                });
                existingMethods.add(findByIdKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Optional.class.getName(), Object.class.getName());
                MethodDesc findByIdDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "findById", findByIdMtd);
                classCreator.method("findById", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("id");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(idTypeStr));
                        Expr result = bbc.invokeVirtual(findByIdDesc, bmc.this_(), castedParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeFindByIdKey);
            }

            allMethodsToBeImplementedToResult.put(findByIdKey, true);
            allMethodsToBeImplementedToResult.put(bridgeFindByIdKey, true);
        }
    }

    private void generateExistsById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String existsByIdKey = GenerationUtil.methodKey("existsById", boolean.class.getName(), idTypeStr);
        String bridgeExistsByIdKey = GenerationUtil.methodKey("existsById", boolean.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(existsByIdKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeExistsByIdKey)) {

            if (!existingMethods.contains(existsByIdKey)) {
                // Force generation of findById
                String findByIdKey = GenerationUtil.methodKey("findById", Optional.class.getName(), idTypeStr);
                allMethodsToBeImplementedToResult.put(findByIdKey, false);
                generateFindById(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr, idTypeStr,
                        allMethodsToBeImplementedToResult, existingMethods);

                MethodTypeDesc findByIdMtd = GenerationUtil.toMethodTypeDesc(Optional.class.getName(), idTypeStr);
                MethodDesc findByIdDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "findById", findByIdMtd);

                MethodTypeDesc existsByIdMtd = GenerationUtil.toMethodTypeDesc(boolean.class.getName(), idTypeStr);
                classCreator.method("existsById", mc -> {
                    mc.setType(existsByIdMtd);
                    ParamVar idParam = mc.parameter("id");
                    mc.body(bc -> {
                        Expr optional = bc.invokeVirtual(findByIdDesc, mc.this_(), idParam);
                        Expr isPresent = bc.invokeVirtual(
                                MethodDesc.of(Optional.class, "isPresent", boolean.class),
                                optional);
                        bc.return_(isPresent);
                    });
                });
                existingMethods.add(existsByIdKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(boolean.class.getName(), Object.class.getName());
                MethodDesc existsByIdDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "existsById", existsByIdMtd);
                classCreator.method("existsById", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("id");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(idTypeStr));
                        Expr result = bbc.invokeVirtual(existsByIdDesc, bmc.this_(), castedParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeExistsByIdKey);
            }

            allMethodsToBeImplementedToResult.put(existsByIdKey, true);
            allMethodsToBeImplementedToResult.put(bridgeExistsByIdKey, true);
        }
    }

    private void generateGetOne(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificFindEntityReference(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                idTypeStr, "getOne", allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateGetById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificFindEntityReference(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                idTypeStr, "getById", allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateGetReferenceById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificFindEntityReference(classCreator, entityClassFieldDescriptor, generatedClassName, entityTypeStr,
                idTypeStr, "getReferenceById", allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateSpecificFindEntityReference(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr, String actualMethodName,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String methodKey = GenerationUtil.methodKey(actualMethodName, entityTypeStr, idTypeStr);
        String bridgeKey = GenerationUtil.methodKey(actualMethodName, Object.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(methodKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeKey)) {

            if (!existingMethods.contains(methodKey)) {
                MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(entityTypeStr, idTypeStr);
                classCreator.method(actualMethodName, mc -> {
                    mc.setType(mtd);
                    ParamVar idParam = mc.parameter("id");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr entity = bc.invokeStatic(
                                MethodDesc.of(RepositorySupport.class, actualMethodName,
                                        Object.class, AbstractManagedJpaOperations.class, Class.class, Object.class),
                                ops, entityClass, idParam);
                        bc.return_(entity);
                    });
                });
                existingMethods.add(methodKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Object.class.getName(), Object.class.getName());
                MethodDesc refDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), actualMethodName, mtd);
                classCreator.method(actualMethodName, bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("id");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(idTypeStr));
                        Expr result = bbc.invokeVirtual(refDesc, bmc.this_(), castedParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeKey);
            }

            allMethodsToBeImplementedToResult.put(methodKey, true);
            allMethodsToBeImplementedToResult.put(bridgeKey, true);
        }
    }

    private void generateFindAll(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<String, Boolean> allMethodsToBeImplementedToResult,
            Set<String> existingMethods) {

        String findAllKey = GenerationUtil.methodKey("findAll", List.class.getName());
        String bridgeFindAllKey = GenerationUtil.methodKey("findAll", Iterable.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(findAllKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllKey)) {

            if (!existingMethods.contains(findAllKey)) {
                classCreator.method("findAll", mc -> {
                    mc.returning(List.class);
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr panacheQuery = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "findAll", Object.class, Class.class),
                                ops, entityClass);
                        Expr list = bc.invokeInterface(
                                MethodDesc.of(PanacheQuery.class, "list", List.class),
                                panacheQuery);
                        bc.return_(list);
                    });
                });
                existingMethods.add(findAllKey);

                // Bridge method
                MethodDesc findAllDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "findAll",
                        MethodTypeDesc.of(ConstantDescs.CD_List));
                classCreator.method("findAll", bmc -> {
                    bmc.returning(Iterable.class);
                    bmc.body(bbc -> {
                        Expr result = bbc.invokeVirtual(findAllDesc, bmc.this_());
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeFindAllKey);
            }

            allMethodsToBeImplementedToResult.put(findAllKey, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllKey, true);
        }
    }

    private void generateFindAllWithSort(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<String, Boolean> allMethodsToBeImplementedToResult,
            Set<String> existingMethods) {

        String findAllKey = GenerationUtil.methodKey("findAll", List.class.getName(), Sort.class.getName());
        String bridgeFindAllKey = GenerationUtil.methodKey("findAll", Iterable.class.getName(), Sort.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(findAllKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllKey)) {

            if (!existingMethods.contains(findAllKey)) {
                MethodTypeDesc findAllMtd = GenerationUtil.toMethodTypeDesc(List.class.getName(), Sort.class.getName());
                classCreator.method("findAll", mc -> {
                    mc.setType(findAllMtd);
                    ParamVar sortParam = mc.parameter("sort");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr sort = bc.invokeStatic(
                                MethodDesc.of(TypesConverter.class, "toPanacheSort",
                                        io.quarkus.panache.common.Sort.class, Sort.class),
                                sortParam);

                        Expr panacheQuery = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "findAll", Object.class, Class.class,
                                        io.quarkus.panache.common.Sort.class),
                                ops, entityClass, sort);
                        Expr list = bc.invokeInterface(
                                MethodDesc.of(PanacheQuery.class, "list", List.class),
                                panacheQuery);
                        bc.return_(list);
                    });
                });
                existingMethods.add(findAllKey);

                // Bridge method
                MethodDesc findAllDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "findAll", findAllMtd);
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Iterable.class.getName(), Sort.class.getName());
                classCreator.method("findAll", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar sortParam = bmc.parameter("sort");
                    bmc.body(bbc -> {
                        Expr result = bbc.invokeVirtual(findAllDesc, bmc.this_(), sortParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeFindAllKey);
            }

            allMethodsToBeImplementedToResult.put(findAllKey, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllKey, true);
        }
    }

    private void generateFindAllWithPageable(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, Map<String, Boolean> allMethodsToBeImplementedToResult,
            Set<String> existingMethods) {

        String findAllKey = GenerationUtil.methodKey("findAll", Page.class.getName(), Pageable.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(findAllKey)) {
            if (!existingMethods.contains(findAllKey)) {
                MethodTypeDesc findAllMtd = GenerationUtil.toMethodTypeDesc(Page.class.getName(), Pageable.class.getName());
                classCreator.method("findAll", mc -> {
                    mc.setType(findAllMtd);
                    ParamVar pageableParam = mc.parameter("pageable");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        LocalVar pageableSort = bc.localVar("pageableSort",
                                bc.invokeInterface(
                                        MethodDesc.of(Pageable.class, "getSort", Sort.class),
                                        pageableParam));
                        LocalVar panachePage = bc.localVar("panachePage",
                                bc.invokeStatic(
                                        MethodDesc.of(TypesConverter.class, "toPanachePage",
                                                io.quarkus.panache.common.Page.class, Pageable.class),
                                        pageableParam));
                        LocalVar panacheSort = bc.localVar("panacheSort",
                                bc.invokeStatic(
                                        MethodDesc.of(TypesConverter.class, "toPanacheSort",
                                                io.quarkus.panache.common.Sort.class, Sort.class),
                                        pageableSort));

                        // Build panacheQuery based on whether sort is null
                        LocalVar panacheQueryVar = bc.localVar("panacheQuery",
                                ConstantDescs.CD_Object,
                                Const.ofNull(ConstantDescs.CD_Object));

                        bc.ifElse(bc.isNull(panacheSort),
                                sortNullTrue -> {
                                    Expr pqWithoutSort = sortNullTrue.invokeVirtual(
                                            MethodDesc.of(AbstractManagedJpaOperations.class, "findAll", Object.class,
                                                    Class.class),
                                            ops, entityClass);
                                    sortNullTrue.set(panacheQueryVar, pqWithoutSort);
                                },
                                sortNullFalse -> {
                                    Expr pqWithSort = sortNullFalse.invokeVirtual(
                                            MethodDesc.of(AbstractManagedJpaOperations.class, "findAll", Object.class,
                                                    Class.class, io.quarkus.panache.common.Sort.class),
                                            ops, entityClass, panacheSort);
                                    sortNullFalse.set(panacheQueryVar, pqWithSort);
                                });

                        LocalVar panacheQuery = bc.localVar("pagedQuery",
                                bc.invokeInterface(
                                        MethodDesc.of(PanacheQuery.class, "page", PanacheQuery.class,
                                                io.quarkus.panache.common.Page.class),
                                        panacheQueryVar, panachePage));
                        Expr list = bc.invokeInterface(
                                MethodDesc.of(PanacheQuery.class, "list", List.class),
                                panacheQuery);
                        Expr count = bc.invokeInterface(
                                MethodDesc.of(PanacheQuery.class, "count", long.class),
                                panacheQuery);
                        Expr pageResult = bc.new_(ClassDesc.of(PageImpl.class.getName()),
                                list, pageableParam, count);

                        bc.return_(pageResult);
                    });
                });
                existingMethods.add(findAllKey);
            }
            allMethodsToBeImplementedToResult.put(findAllKey, true);
        }
    }

    private void generateFindAllById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, DotName entityDotName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String findAllByIdKey = GenerationUtil.methodKey("findAllById", List.class.getName(), Iterable.class.getName());
        String bridgeFindAllByIdKey = GenerationUtil.methodKey("findAllById", Iterable.class.getName(),
                Iterable.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(findAllByIdKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeFindAllByIdKey)) {

            if (!existingMethods.contains(findAllByIdKey)) {
                MethodTypeDesc findAllByIdMtd = GenerationUtil.toMethodTypeDesc(List.class.getName(),
                        Iterable.class.getName());
                classCreator.method("findAllById", mc -> {
                    mc.setType(findAllByIdMtd);
                    ParamVar idsParam = mc.parameter("ids");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr list = bc.invokeStatic(
                                MethodDesc.of(RepositorySupport.class, "findByIds",
                                        List.class, AbstractManagedJpaOperations.class, Class.class, Iterable.class),
                                ops, entityClass,
                                idsParam);
                        bc.return_(list);
                    });
                });
                existingMethods.add(findAllByIdKey);

                // Bridge method
                MethodDesc findAllByIdDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "findAllById",
                        findAllByIdMtd);
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(Iterable.class.getName(),
                        Iterable.class.getName());
                classCreator.method("findAllById", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("ids");
                    bmc.body(bbc -> {
                        Expr result = bbc.invokeVirtual(findAllByIdDesc, bmc.this_(), methodParam);
                        bbc.return_(result);
                    });
                });
                existingMethods.add(bridgeFindAllByIdKey);
            }

            allMethodsToBeImplementedToResult.put(findAllByIdKey, true);
            allMethodsToBeImplementedToResult.put(bridgeFindAllByIdKey, true);
        }
    }

    private void generateCount(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor, String generatedClassName,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String countKey = GenerationUtil.methodKey("count", long.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(countKey)) {
            if (!existingMethods.contains(countKey)) {
                classCreator.method("count", mc -> {
                    mc.returning(long.class);
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr result = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "count", long.class, Class.class),
                                ops, entityClass);
                        bc.return_(result);
                    });
                });
                existingMethods.add(countKey);
            }
            allMethodsToBeImplementedToResult.put(countKey, true);
        }
    }

    private void generateDeleteById(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, String entityTypeStr, String idTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String deleteByIdKey = GenerationUtil.methodKey("deleteById", void.class.getName(), idTypeStr);
        String bridgeDeleteByIdKey = GenerationUtil.methodKey("deleteById", void.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(deleteByIdKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeDeleteByIdKey)) {

            if (!existingMethods.contains(deleteByIdKey)) {
                MethodTypeDesc deleteByIdMtd = GenerationUtil.toMethodTypeDesc(void.class.getName(), idTypeStr);
                classCreator.method("deleteById", mc -> {
                    mc.setType(deleteByIdMtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar idParam = mc.parameter("id");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        Expr deleted = bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "deleteById", boolean.class,
                                        Class.class, Object.class),
                                ops, entityClass, idParam);

                        bc.ifNot(deleted, deletedFalse -> {
                            Expr idToString = deletedFalse.invokeVirtual(
                                    MethodDesc.of(Object.class, "toString", String.class),
                                    idParam);
                            Expr formatArgsArray = deletedFalse.newArray(Object.class, idToString);

                            Expr messageFormat = Const.of("No entity " + entityTypeStr + " with id %s exists");
                            Expr message = deletedFalse.invokeStatic(
                                    MethodDesc.of(String.class, "format", String.class, String.class, Object[].class),
                                    messageFormat, formatArgsArray);

                            deletedFalse.throw_(IllegalArgumentException.class, message);
                        });

                        bc.return_();
                    });
                });
                existingMethods.add(deleteByIdKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(void.class.getName(), Object.class.getName());
                MethodDesc deleteByIdDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "deleteById", deleteByIdMtd);
                classCreator.method("deleteById", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("id");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(idTypeStr));
                        bbc.invokeVirtual(deleteByIdDesc, bmc.this_(), castedParam);
                        bbc.return_();
                    });
                });
                existingMethods.add(bridgeDeleteByIdKey);
            }

            allMethodsToBeImplementedToResult.put(deleteByIdKey, true);
            allMethodsToBeImplementedToResult.put(bridgeDeleteByIdKey, true);
        }
    }

    private void generateDelete(ClassCreator classCreator, String generatedClassName, String entityTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String deleteKey = GenerationUtil.methodKey("delete", void.class.getName(), entityTypeStr);
        String bridgeDeleteKey = GenerationUtil.methodKey("delete", void.class.getName(), Object.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(deleteKey)
                || allMethodsToBeImplementedToResult.containsKey(bridgeDeleteKey)) {

            if (!existingMethods.contains(deleteKey)) {
                MethodTypeDesc deleteMtd = GenerationUtil.toMethodTypeDesc(void.class.getName(), entityTypeStr);
                classCreator.method("delete", mc -> {
                    mc.setType(deleteMtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar entityParam = mc.parameter("entity");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "delete", void.class, Object.class),
                                ops, entityParam);
                        bc.return_();
                    });
                });
                existingMethods.add(deleteKey);

                // Bridge method
                MethodTypeDesc bridgeMtd = GenerationUtil.toMethodTypeDesc(void.class.getName(), Object.class.getName());
                MethodDesc deleteDesc = ClassMethodDesc.of(ClassDesc.of(generatedClassName), "delete", deleteMtd);
                classCreator.method("delete", bmc -> {
                    bmc.setType(bridgeMtd);
                    ParamVar methodParam = bmc.parameter("entity");
                    bmc.body(bbc -> {
                        Expr castedParam = bbc.cast(methodParam, ClassDesc.of(entityTypeStr));
                        bbc.invokeVirtual(deleteDesc, bmc.this_(), castedParam);
                        bbc.return_();
                    });
                });
                existingMethods.add(bridgeDeleteKey);
            }
        }

        allMethodsToBeImplementedToResult.put(deleteKey, true);
        allMethodsToBeImplementedToResult.put(bridgeDeleteKey, true);
    }

    private void generateDeleteInBatchWithIterable(ClassCreator classCreator, String generatedClassName, String entityTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, "deleteInBatch",
                allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateDeleteAllInBatchWithIterable(ClassCreator classCreator, String generatedClassName,
            String entityTypeStr, Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, "deleteAllInBatch",
                allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateDeleteAllByIdInBatchWithIterable(ClassCreator classCreator, String generatedClassName,
            String entityTypeStr, Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, "deleteAllByIdInBatch",
                allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateDeleteAllWithIterable(ClassCreator classCreator, String generatedClassName, String entityTypeStr,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        generateSpecificDeleteAllWithIterable(classCreator, generatedClassName, entityTypeStr, "deleteAll",
                allMethodsToBeImplementedToResult, existingMethods);
    }

    private void generateSpecificDeleteAllWithIterable(ClassCreator classCreator, String generatedClassName,
            String entityTypeStr, String actualMethodName,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {

        String key = GenerationUtil.methodKey(actualMethodName, void.class.getName(), Iterable.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(key)) {
            if (!existingMethods.contains(key)) {
                MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(void.class.getName(), Iterable.class.getName());
                classCreator.method(actualMethodName, mc -> {
                    mc.setType(mtd);
                    mc.addAnnotation(Transactional.class);
                    ParamVar entitiesParam = mc.parameter("entities");
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        bc.invokeStatic(
                                MethodDesc.of(RepositorySupport.class, "deleteAll",
                                        void.class, AbstractManagedJpaOperations.class, Iterable.class),
                                ops,
                                entitiesParam);
                        bc.return_();
                    });
                });
                existingMethods.add(key);
            }
            allMethodsToBeImplementedToResult.put(key, true);
        }
    }

    private void generateDeleteAll(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, Map<String, Boolean> allMethodsToBeImplementedToResult,
            Set<String> existingMethods) {

        String deleteAllKey = GenerationUtil.methodKey("deleteAll", void.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(deleteAllKey)) {
            if (!existingMethods.contains(deleteAllKey)) {
                classCreator.method("deleteAll", mc -> {
                    mc.returning(void.class);
                    mc.addAnnotation(Transactional.class);
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        bc.invokeStatic(
                                MethodDesc.of(AdditionalJpaOperations.class, "deleteAllWithCascade", long.class,
                                        AbstractManagedJpaOperations.class, Class.class),
                                ops, entityClass);
                        bc.return_();
                    });
                });
                existingMethods.add(deleteAllKey);
            }
            allMethodsToBeImplementedToResult.put(deleteAllKey, true);
        }
    }

    private void generateDeleteAllInBatch(ClassCreator classCreator, FieldDesc entityClassFieldDescriptor,
            String generatedClassName, Map<String, Boolean> allMethodsToBeImplementedToResult,
            Set<String> existingMethods) {

        String deleteAllInBatchKey = GenerationUtil.methodKey("deleteAllInBatch", void.class.getName());

        if (allMethodsToBeImplementedToResult.containsKey(deleteAllInBatchKey)) {
            if (!existingMethods.contains(deleteAllInBatchKey)) {
                classCreator.method("deleteAllInBatch", mc -> {
                    mc.returning(void.class);
                    mc.addAnnotation(Transactional.class);
                    mc.body(bc -> {
                        LocalVar ops = bc.localVar("ops", bc.getStaticField(operationsField));
                        LocalVar entityClass = bc.localVar("entityClass",
                                bc.get(mc.this_().field(entityClassFieldDescriptor)));
                        bc.invokeVirtual(
                                MethodDesc.of(AbstractManagedJpaOperations.class, "deleteAll", long.class, Class.class),
                                ops, entityClass);
                        bc.return_();
                    });
                });
                existingMethods.add(deleteAllInBatchKey);
            }
            allMethodsToBeImplementedToResult.put(deleteAllInBatchKey, true);
        }
    }

    private void handleUnimplementedMethods(ClassCreator classCreator, String generatedClassName,
            Map<String, Boolean> allMethodsToBeImplementedToResult, Set<String> existingMethods) {
        for (Map.Entry<String, Boolean> entry : allMethodsToBeImplementedToResult.entrySet()) {
            if (entry.getValue()) { // ignore implemented methods
                continue;
            }

            // Parse the method key to get name and types
            String key = entry.getKey();
            int parenOpen = key.indexOf('(');
            int parenClose = key.indexOf(')');
            String methodName = key.substring(0, parenOpen);
            String paramsPart = key.substring(parenOpen + 1, parenClose);
            String returnType = key.substring(parenClose + 1);

            String[] paramTypes = paramsPart.isEmpty() ? new String[0] : paramsPart.split(",");

            if (existingMethods.contains(key)) {
                continue;
            }

            MethodTypeDesc mtd = GenerationUtil.toMethodTypeDesc(returnType, paramTypes);
            classCreator.method(methodName, mc -> {
                mc.setType(mtd);
                for (int i = 0; i < paramTypes.length; i++) {
                    mc.parameter("p" + i);
                }
                mc.body(bc -> {
                    Expr res = bc.new_(ClassDesc.of(FunctionalityNotImplemented.class.getName()),
                            Const.of(generatedClassName.replace('/', '.')),
                            Const.of(methodName));
                    bc.throw_(res);
                });
            });
            existingMethods.add(key);
        }
    }

    private Set<MethodInfo> methodsOfExtendedSpringDataRepositories(ClassInfo repositoryToImplement) {
        return GenerationUtil.interfaceMethods(GenerationUtil.extendedSpringDataRepos(repositoryToImplement, index), index);
    }

    private Set<MethodInfo> stockMethodsAddedToInterface(ClassInfo repositoryToImplement) {
        Set<MethodInfo> result = new LinkedHashSet<>();

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

    private boolean canMethodsBeConsideredSame(MethodInfo candidate, MethodInfo target) {
        if (!candidate.name().equals(target.name())) {
            return false;
        }

        if (candidate.parametersCount() != target.parametersCount()) {
            return false;
        }

        if (!canTypesBeConsideredSame(candidate.returnType(), target.returnType())) {
            return false;
        }

        for (int i = 0; i < candidate.parametersCount(); i++) {
            if (!canTypesBeConsideredSame(candidate.parameterType(i), target.parameterType(i))) {
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

        List<AnnotationInstance> annotationInstances = Stream.of(DotNames.JPA_ID, DotNames.JPA_EMBEDDED_ID)
                .map(classInfo.annotationsMap()::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (annotationInstances.isEmpty()) {
            if (DotNames.OBJECT.equals(classInfo.superName())) {
                throw new IllegalArgumentException(
                        "Currently only Entities with the @Id or @EmbeddedId annotation are supported. Offending class is "
                                + originalEntityDotName);
            }
            return getIdAnnotationTargetRec(classInfo.superName(), index, originalEntityDotName);
        }

        if (annotationInstances.size() > 1) {
            throw new IllegalArgumentException(
                    "Currently the @Id or @EmbeddedId annotation can only be placed on a single field or method. " +
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

        if (!classInfo.annotationsMap().containsKey(DotNames.VERSION)) {
            if (DotNames.OBJECT.equals(classInfo.superName())) {
                return Optional.empty();
            }
            return getVersionAnnotationTargetRec(classInfo.superName(), index, originalEntityDotName);
        }

        List<AnnotationInstance> annotationInstances = classInfo.annotationsMap().get(DotNames.VERSION);
        if (annotationInstances.size() > 1) {
            throw new IllegalArgumentException(
                    "Currently the @Version annotation can only be placed on a single field or method. " +
                            "Offending class is " + originalEntityDotName);
        }

        return Optional.of(annotationInstances.get(0).target());
    }
}
