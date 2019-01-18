/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.panache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.panache.Controller;
import org.jboss.panache.EntityBase;
import org.jboss.panache.Model;
import org.jboss.panache.PgPoolProducer;
import org.jboss.panache.RxEntityBase;
import org.jboss.panache.RxModel;
import org.jboss.protean.gizmo.AssignableResultHandle;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.FunctionCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.jpa.AdditionalJpaModelBuildItem;

import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import net.bytebuddy.jar.asm.Opcodes;

/**
 */
public final class ModelResourceProcessor {

    private enum RelationType {
        ONE, MANY;
    }
    
    public class RxField {

        private String name;
        private Class<?> type;
        private Class<?> entityClass;
        private RelationType relationType;
        private String reverseField;

        public RxField(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public RxField(String name, Class<?> type, Class<?> entityClass) {
            this(name, type);
            this.relationType = RelationType.ONE;
            this.entityClass = entityClass;
        }

        public RxField(String name, Class<?> type, Class<?> entityClass, String mappedBy) {
            this(name, type);
            this.relationType = RelationType.MANY;
            this.entityClass = entityClass;
            this.reverseField = mappedBy;
        }

        public String getFromRowMethod() {
            if(type == String.class)
                return "getString";
            if(type == Boolean.class)
                return "getBoolean";
            if(type == Short.class)
                return "getShort";
            if(type == Integer.class
                    || type.isEnum()
                    || isManyToOne())
                return "getInteger";
            if(type == Long.class)
                return "getLong";
            if(type == Float.class)
                return "getFloat";
            if(type == Double.class)
                return "getDouble";
            throw new RuntimeException("Field type not supported yet: "+type+" for field "+name);
        }

        public Class<?> mappedType() {
            if(type.isEnum() || isManyToOne())
                return Integer.class;
            return type;
        }

        public boolean isManyToOne() {
            return relationType == RelationType.ONE;
        }

        public boolean isOneToMany() {
            return relationType == RelationType.MANY;
        }

        public String columnName() {
            if(isManyToOne())
                return name + "_id";
            return name;
        }
    }

    private static final DotName DOTNAME_CONTROLLER_BASE = DotName.createSimple(Controller.class.getName());
    private static final DotName DOTNAME_ENTITY_BASE = DotName.createSimple(EntityBase.class.getName());
    private static final DotName DOTNAME_RX_ENTITY_BASE = DotName.createSimple(RxEntityBase.class.getName());
    private static final DotName DOTNAME_MODEL = DotName.createSimple(Model.class.getName());
    private static final DotName DOTNAME_RX_MODEL = DotName.createSimple(RxModel.class.getName());
    
    @BuildStep
    List<AdditionalJpaModelBuildItem> produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return Arrays.asList(
                new AdditionalJpaModelBuildItem(Model.class));
    }
    
    @BuildStep
    AdditionalBeanBuildItem producePgPool() {
        return new AdditionalBeanBuildItem(PgPoolProducer.class);
    }
    
    @BuildStep
    void build(CombinedIndexBuildItem index,
               BuildProducer<BytecodeTransformerBuildItem> transformers,
               BuildProducer<GeneratedClassBuildItem> generatedClasses) throws Exception {

        RouterEnhancer routerEnhancer = new RouterEnhancer();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_CONTROLLER_BASE)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), routerEnhancer));
        }

        ModelEnhancer modelEnhancer = new ModelEnhancer();
        Set<String> modelClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_ENTITY_BASE)) {
            // skip Model
            if(classInfo.name().equals(DOTNAME_MODEL))
                continue;
            modelClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_MODEL)) {
            modelClasses.add(classInfo.name().toString());
        }
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));
        }
        
        RxModelEnhancer rxModelEnhancer = new RxModelEnhancer();
        Set<String> rxModelClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_RX_ENTITY_BASE)) {
            // skip RxModel
            if(classInfo.name().equals(DOTNAME_RX_MODEL))
                continue;
            rxModelClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_RX_MODEL)) {
            rxModelClasses.add(classInfo.name().toString());
        }
        for (String rxModelClass : rxModelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(rxModelClass, rxModelEnhancer));
            generateModelClass(rxModelClass, generatedClasses);
        }

        // this just deadlocks, probably fighting with JPA
//        System.out.println("EXPORT C");
//        try {
//            final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
//            System.out.println("EXPORT1aa");
//            final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );
//            System.out.println("EXPORT1a");
//            Properties properties = new Properties();
//            System.out.println("EXPORT1b");
//            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
//            System.out.println("EXPORT1c");
//            ssrBuilder.applySettings(properties);
//            
//            SchemaExport export = new SchemaExport();
//            System.out.println("EXPORT1");
//            MetadataSources sources = new MetadataSources(ssrBuilder.build());
//            System.out.println("EXPORT2");
//            sources.addAnnotatedClass(RxModel.class);
//            System.out.println("EXPORT3");
//            for (String rxModelClass : rxModelClasses) {
//                sources.addAnnotatedClassName(rxModelClass);
//            }
//            System.out.println("EXPORT4");
//            Metadata metadata = sources.getMetadataBuilder().build();
//            System.out.println("EXPORT5");
//            export.create(EnumSet.of(TargetType.STDOUT), metadata);
//        }catch(Throwable t) {
//            t.printStackTrace();
//        }
//        System.out.println("EXPORT END");
    }

    private void generateModelClass(String modelClassName, BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        
        String modelType = modelClassName.replace('.', '/');
        String modelSignature = "L"+modelType+";";
        // FIXME
        String tableName;
        int lastDot = modelClassName.lastIndexOf('.');
        if(lastDot != -1)
            tableName = modelClassName.substring(lastDot+1);
        else
            tableName = modelClassName;
        
        String modelInfoClassName = modelClassName+"$__MODEL";
        
        ClassCreator modelClass = ClassCreator.builder().className(modelInfoClassName)
            .classOutput(new ProcessorClassOutput(generatedClasses))
            .interfaces(RxEntityBase.RxModelInfo.class)
            .signature("Ljava/lang/Object;Lorg/jboss/panache/RxEntityBase$RxModelInfo<"+modelSignature+">;")
            .build();
        
        // no arg constructor is auto-created by gizmo
        
        // getEntityClass
        MethodCreator getEntityClass = modelClass.getMethodCreator("getEntityClass", Class.class);
        getEntityClass.returnValue(getEntityClass.loadClass(modelClassName));
        
        List<RxField> fields = new ArrayList<>();
        try {
            loadFields(Class.forName(modelClassName), fields );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        createFromRow(modelClass, modelClassName, modelSignature, fields);
        
        // insertStatement
        MethodCreator insertStatement = modelClass.getMethodCreator("insertStatement", String.class);
        StringBuilder names = new StringBuilder();
        StringBuilder indices = new StringBuilder();
        StringBuilder updateFieldNoId = new StringBuilder();
        int manyToOnes = 0;
        for (int i = 0; i < fields.size(); i++) {
            RxField field = fields.get(i);
            // skip collections
            if(field.isOneToMany())
                continue;
            if(names.length() != 0) {
                names.append(", ");
                indices.append(", ");
            }
            if(updateFieldNoId.length() != 0) {
                updateFieldNoId.append(", ");
            }
            if(field.isManyToOne())
                manyToOnes++;
            names.append(field.columnName());
            indices.append("$"+(i+1));
            // FIXME: depends on ID being the first field
            if(i > 0) {
                updateFieldNoId.append(field.columnName()+" = $"+(i+1));
            }
        }
        insertStatement.returnValue(insertStatement.load("INSERT INTO "+tableName+" ("+names+") VALUES ("+indices+")"));

        // updateStatement
        MethodCreator updateStatement = modelClass.getMethodCreator("updateStatement", String.class);
        // FIXME: do not hardcode the ID
        updateStatement.returnValue(updateStatement.load("UPDATE "+tableName+" SET "+updateFieldNoId+" WHERE id = $1"));

        // getTableName
        MethodCreator getTableName = modelClass.getMethodCreator("getTableName", String.class);
        getTableName.returnValue(getTableName.load(tableName));

        // toTuple
        createToTuple(modelClass, modelClassName, fields, manyToOnes);
        
        // Bridge methods
        MethodCreator toTupleBridge = modelClass.getMethodCreator("toTuple", Single.class, RxEntityBase.class);
        toTupleBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        toTupleBridge.returnValue(toTupleBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "toTuple", 
                                                                                              Single.class, modelClassName), 
                                                                    toTupleBridge.getThis(), 
                                                                    toTupleBridge.checkCast(toTupleBridge.getMethodParam(0), modelClassName)));
        
        MethodCreator fromRowBridge = modelClass.getMethodCreator("fromRow", RxEntityBase.class, Row.class);
        fromRowBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        fromRowBridge.returnValue(fromRowBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "fromRow", 
                                                                                              modelClassName, Row.class), 
                                                                    fromRowBridge.getThis(), 
                                                                    fromRowBridge.getMethodParam(0)));
        
        modelClass.close();
    }

    private void createFromRow(ClassCreator modelClass, String modelClassName, String modelSignature, List<RxField> fields) {
        // fromRow
        MethodCreator fromRow = modelClass.getMethodCreator("fromRow", modelClassName, Row.class.getName());
        AssignableResultHandle variable = fromRow.createVariable(modelSignature);
        // arg-less constructor
        fromRow.assign(variable, fromRow.newInstance(MethodDescriptor.ofConstructor(modelClassName)));
        
        // set each field from the Row
        for (RxField field : fields) {
            ResultHandle value;
            AssignableResultHandle fieldValue = fromRow.createVariable(field.type);
            if(field.isOneToMany()) {
//              RxDog.<RxDog>find("owner_id = ?1", id).cache();
                ResultHandle array = fromRow.newArray(Object.class, fromRow.load(1));
                fromRow.writeArrayValue(array, 0, fromRow.readInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), variable));
                ResultHandle obs = fromRow.invokeStaticMethod(MethodDescriptor.ofMethod(field.entityClass, "find", Observable.class, String.class, Object[].class), 
                        fromRow.load(field.reverseField+"_id = ?1"), array);
                value = fromRow.invokeVirtualMethod(MethodDescriptor.ofMethod(Observable.class, "cache", Observable.class), obs);
                fromRow.assign(fieldValue, value);
            }else {
                value = fromRow.invokeVirtualMethod(MethodDescriptor.ofMethod(Row.class, field.getFromRowMethod(), field.mappedType(), String.class), 
                        fromRow.getMethodParam(0), fromRow.load(field.columnName()));
                if(field.type.isEnum()) {
                    BranchResult branch = fromRow.ifNull(value);

                    branch.trueBranch().assign(fieldValue, branch.trueBranch().loadNull());
                    branch.trueBranch().close();

                    ResultHandle enumValues = branch.falseBranch().invokeStaticMethod(MethodDescriptor.ofMethod(field.type.getName(), "values", "[L"+field.type.getName()+";"));
                    value = branch.falseBranch().readArrayValue(enumValues, branch.falseBranch().invokeVirtualMethod(MethodDescriptor.ofMethod(Integer.class, "intValue", int.class), value));
                    branch.falseBranch().assign(fieldValue, value);
                    branch.falseBranch().close();
                } else if(field.isManyToOne()) {
                    FunctionCreator deferred = fromRow.createFunction(Callable.class);
                    BytecodeCreator deferredCreator = deferred.getBytecode();
                    ResultHandle byId = deferredCreator.invokeStaticMethod(MethodDescriptor.ofMethod(field.entityClass, "findById", Maybe.class, Object.class), value);
                    ResultHandle toSingle = deferredCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Maybe.class, "toSingle", Single.class), byId);
                    ResultHandle cached = deferredCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Single.class, "cache", Single.class), toSingle);
                    deferredCreator.returnValue(cached);
                    fromRow.assign(fieldValue, fromRow.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Single.class, "defer", Single.class, Callable.class), deferred.getInstance()));
                } else {
                    fromRow.assign(fieldValue, value);
                }
            }
            fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, field.name, field.type), variable, fieldValue);
        }
        fromRow.returnValue(variable);
    }

    private void createToTuple(ClassCreator modelClass, String modelClassName, List<RxField> fields, int manyToOnes) {
        MethodCreator toTuple = modelClass.getMethodCreator("toTuple", Single.class, modelClassName);
        ResultHandle entityParam = toTuple.getMethodParam(0);

        BytecodeCreator creator = toTuple;
        FunctionCreator myFunction = null;
        if(manyToOnes > 0) {
            myFunction = toTuple.createFunction(Function.class);
            creator = myFunction.getBytecode();
        }
        // FIXME: do not hardcode the ID
        ResultHandle myTuple = creator.invokeStaticMethod(MethodDescriptor.ofMethod(Tuple.class, "tuple", Tuple.class));

        BranchResult branch = creator.ifNull(creator.readInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), 
                entityParam));
        branch.trueBranch().close();
        ResultHandle idFieldValue = branch.falseBranch().readInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), 
                entityParam);
        branch.falseBranch().invokeVirtualMethod(MethodDescriptor.ofMethod(Tuple.class, "addValue", Tuple.class, Object.class), myTuple, idFieldValue);
        branch.falseBranch().close();
        
        // skip the ID field
        for (int j = 1, entityField = 0; j < fields.size(); j++) {
            RxField field = fields.get(j);
            // skip collections
            if(field.isOneToMany())
                continue;
            ResultHandle fieldValue;
            if(field.isManyToOne()) {
                fieldValue = creator.readInstanceField(FieldDescriptor.of(RxModel.class, "id", Integer.class),
                        creator.checkCast(creator.readArrayValue(creator.checkCast(creator.getMethodParam(0), Object[].class), entityField++),
                                field.entityClass));
            } else {
                fieldValue = creator.readInstanceField(FieldDescriptor.of(modelClassName, field.name, field.type), 
                        entityParam);
            }
            if(field.type.isEnum()) {
                // FIXME: handle NPE
                fieldValue = creator.invokeVirtualMethod(MethodDescriptor.ofMethod(Enum.class, "ordinal", int.class), fieldValue);
            }
            creator.invokeVirtualMethod(MethodDescriptor.ofMethod(Tuple.class, "addValue", Tuple.class, Object.class), myTuple, fieldValue);
        }

        if(manyToOnes > 0) {
            creator.returnValue(myTuple);

            AssignableResultHandle myArgs = toTuple.createVariable(SingleSource[].class);
            toTuple.assign(myArgs, toTuple.newArray(SingleSource[].class, toTuple.load(manyToOnes)));
            int i = 0;
            for (RxField field : fields) {
                if(!field.isManyToOne())
                    continue;
                toTuple.writeArrayValue(myArgs, i, toTuple.readInstanceField(FieldDescriptor.of(modelClassName, field.name, field.type), 
                        entityParam));
            }

            toTuple.returnValue(toTuple.invokeStaticMethod(MethodDescriptor.ofMethod(Single.class, "zipArray", Single.class, Function.class, SingleSource[].class), 
                    myFunction.getInstance(), myArgs));
        } else {
            creator.returnValue(creator.invokeStaticMethod(MethodDescriptor.ofMethod(Single.class, "just", Single.class, Object.class), 
                    myTuple));
        }
    }

    private void loadFields(Class<?> modelClass, List<RxField> fields) {
        Class<?> superClass = modelClass.getSuperclass();
        if(superClass != null
                && superClass != RxEntityBase.class)
            loadFields(superClass, fields);
        for (Field field : modelClass.getDeclaredFields()) {
            if(Modifier.isTransient(field.getModifiers())
                    || field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            if(field.isAnnotationPresent(ManyToOne.class)) {
                // FIXME: that stinks
                ParameterizedType fieldType = (ParameterizedType)field.getGenericType();
                Class<?> entityClass = (Class<?>) fieldType.getActualTypeArguments()[0];
                fields.add(new RxField(field.getName(), field.getType(), entityClass));
            }else if(field.isAnnotationPresent(OneToMany.class)) {
                // FIXME: that stinks
                ParameterizedType fieldType = (ParameterizedType)field.getGenericType();
                Class<?> entityClass = (Class<?>) fieldType.getActualTypeArguments()[0];
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                fields.add(new RxField(field.getName(), field.getType(), entityClass, oneToMany.mappedBy()));
            }else {
                fields.add(new RxField(field.getName(), field.getType()));
            }
        }
        // FIXME: add properties (wait for Hibernate code?)
    }

    static final class ProcessorClassOutput implements ClassOutput {
        private final BuildProducer<GeneratedClassBuildItem> producer;

        ProcessorClassOutput(BuildProducer<GeneratedClassBuildItem> producer) {
            this.producer = producer;
        }

        public void write(final String name, final byte[] data) {
            producer.produce(new GeneratedClassBuildItem(false, name, data));
        }

    }}
