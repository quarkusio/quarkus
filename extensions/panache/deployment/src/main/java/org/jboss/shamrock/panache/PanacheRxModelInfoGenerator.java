package org.jboss.shamrock.panache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.jboss.panache.RxEntityBase;
import org.jboss.panache.RxModel;
import org.jboss.protean.gizmo.AssignableResultHandle;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.FunctionCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.panache.PanacheResourceProcessor.ProcessorClassOutput;

import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import net.bytebuddy.jar.asm.Opcodes;

public class PanacheRxModelInfoGenerator {
    
    private enum RelationType {
        ONE, MANY;
    }
    
    private static class RxField {

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

    public static void generateModelClass(String modelClassName, BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        
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
            .signature("Ljava/lang/Object;L"+PanacheRxModelEnhancer.RX_MODEL_INFO_BINARY_NAME+"<"+modelSignature+">;")
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

    private static void createFromRow(ClassCreator modelClass, String modelClassName, String modelSignature, List<RxField> fields) {
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

    private static void createToTuple(ClassCreator modelClass, String modelClassName, List<RxField> fields, int manyToOnes) {
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

    private static void loadFields(Class<?> modelClass, List<RxField> fields) {
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

}
