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

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.panache.Model;
import org.jboss.panache.PgPoolProducer;
import org.jboss.panache.RxModel;
import org.jboss.protean.gizmo.AssignableResultHandle;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.jpa.AdditionalJpaModelBuildItem;

import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import net.bytebuddy.jar.asm.Opcodes;

/**
 */
public final class ModelResourceProcessor {

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

        ModelEnhancer modelEnhancer = new ModelEnhancer();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_MODEL)) {
            System.err.println("Scanning model class for bytecode work: "+classInfo);
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), modelEnhancer));
        }
        RxModelEnhancer rxModelEnhancer = new RxModelEnhancer();
        for (ClassInfo classInfo : index.getIndex().getKnownDirectSubclasses(DOTNAME_RX_MODEL)) {
            System.err.println("Scanning RX model class for bytecode work: "+classInfo);
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), rxModelEnhancer));
            generateModelClass(classInfo, generatedClasses);
        }
    }

    private void generateModelClass(ClassInfo classInfo, BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        
        String modelClassName = classInfo.name().toString();
        String modelType = modelClassName.replace('.', '/');
        String modelSignature = "L"+modelType+";";
        // FIXME
        String tableName;
        int lastDot = modelClassName.lastIndexOf('.');
        if(lastDot != -1)
            tableName = modelClassName.substring(lastDot+1);
        else
            tableName = modelClassName;
        
        String modelInfoClassName = classInfo.name().toString()+"$__MODEL";
        
        ClassCreator modelClass = ClassCreator.builder().className(modelInfoClassName)
            .classOutput(new ProcessorClassOutput(generatedClasses))
            .interfaces(RxModel.RxModelInfo.class)
            .signature("Ljava/lang/Object;Lorg/jboss/panache/RxModel$RxModelInfo<"+modelSignature+">;")
            .build();
        
        // no arg constructor is auto-created by gizmo
        
        // getEntityClass
        MethodCreator getEntityClass = modelClass.getMethodCreator("getEntityClass", Class.class);
        getEntityClass.returnValue(getEntityClass.loadClass(modelClassName));
        
        // fromRow
        MethodCreator fromRow = modelClass.getMethodCreator("fromRow", modelClassName, Row.class.getName());
        AssignableResultHandle variable = fromRow.createVariable(modelSignature);
        // arg-less constructor
        fromRow.assign(variable, fromRow.newInstance(MethodDescriptor.ofConstructor(modelClassName)));
        // set each field from the Row
        // FIXME: do not hardcode the fields
        fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), variable, 
                                   fromRow.invokeVirtualMethod(MethodDescriptor.ofMethod(Row.class, "getInteger", Integer.class, String.class), 
                                                                 fromRow.getMethodParam(0), fromRow.load("id")));
        fromRow.writeInstanceField(FieldDescriptor.of(modelClassName, "name", String.class), variable, 
                                   fromRow.invokeVirtualMethod(MethodDescriptor.ofMethod(Row.class, "getString", String.class, String.class), 
                                                                 fromRow.getMethodParam(0), fromRow.load("name")));
        fromRow.returnValue(variable);
        // FIXME: required bridge?
        
        // insertStatement
        MethodCreator insertStatement = modelClass.getMethodCreator("insertStatement", String.class);
        // FIXME: do not hardcode the fields
        insertStatement.returnValue(insertStatement.load("INSERT INTO "+tableName+" (id, name) VALUES ($1, $2)"));

        // updateStatement
        MethodCreator updateStatement = modelClass.getMethodCreator("updateStatement", String.class);
        // FIXME: do not hardcode the fields
        updateStatement.returnValue(updateStatement.load("UPDATE "+tableName+" SET name = $2 WHERE id = $1"));

        // getTableName
        MethodCreator getTableName = modelClass.getMethodCreator("getTableName", String.class);
        getTableName.returnValue(getTableName.load(tableName));

        // toTuple
        MethodCreator toTuple = modelClass.getMethodCreator("toTuple", Tuple.class.getName(), modelClassName);
        // FIXME: do not hardcode the fields
        BranchResult branch = toTuple.ifNull(toTuple.readInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), 
                                                                                 toTuple.getMethodParam(0)));
        
        branch.trueBranch().returnValue(branch.trueBranch().invokeStaticMethod(MethodDescriptor.ofMethod(Tuple.class, "of", Tuple.class, Object.class), 
                                                                               branch.trueBranch().readInstanceField(FieldDescriptor.of(modelClassName, "name", String.class), 
                                                                                                                     branch.trueBranch().getMethodParam(0))));
        branch.falseBranch().returnValue(branch.falseBranch().invokeStaticMethod(MethodDescriptor.ofMethod(Tuple.class, "of", Tuple.class, Object.class, Object.class), 
                                                                                 branch.falseBranch().readInstanceField(FieldDescriptor.of(modelClassName, "id", Integer.class), 
                                                                                                                        branch.falseBranch().getMethodParam(0)),
                                                                                 branch.falseBranch().readInstanceField(FieldDescriptor.of(modelClassName, "name", String.class), 
                                                                                                                        branch.falseBranch().getMethodParam(0))));
        // Bridge methods
        MethodCreator toTupleBridge = modelClass.getMethodCreator("toTuple", Tuple.class, RxModel.class);
        toTupleBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        toTupleBridge.returnValue(toTupleBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "toTuple", 
                                                                                              Tuple.class.getName(), modelClassName), 
                                                                    toTupleBridge.getThis(), 
                                                                    toTupleBridge.checkCast(toTupleBridge.getMethodParam(0), modelClassName)));
        
        MethodCreator fromRowBridge = modelClass.getMethodCreator("fromRow", RxModel.class, Row.class);
        fromRowBridge.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
        fromRowBridge.returnValue(fromRowBridge.invokeVirtualMethod(MethodDescriptor.ofMethod(modelInfoClassName, "fromRow", 
                                                                                              modelClassName, Row.class.getName()), 
                                                                    fromRowBridge.getThis(), 
                                                                    fromRowBridge.getMethodParam(0)));
        
        modelClass.close();
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
