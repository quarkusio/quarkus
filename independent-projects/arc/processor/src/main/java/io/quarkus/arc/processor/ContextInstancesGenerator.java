package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.impl.ContextInstances;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.Switch.StringSwitch;
import io.quarkus.gizmo.TryBlock;

public class ContextInstancesGenerator extends AbstractGenerator {

    static final String CONTEXT_INSTANCES_SUFFIX = "_ContextInstances";

    private final BeanDeployment beanDeployment;
    private final Map<DotName, String> scopeToGeneratedName;

    public ContextInstancesGenerator(boolean generateSources, ReflectionRegistration reflectionRegistration,
            BeanDeployment beanDeployment, Map<DotName, String> scopeToGeneratedName) {
        super(generateSources, reflectionRegistration);
        this.beanDeployment = beanDeployment;
        this.scopeToGeneratedName = scopeToGeneratedName;
    }

    void precomputeGeneratedName(DotName scope) {
        String generatedName = DEFAULT_PACKAGE + "." + beanDeployment.name + UNDERSCORE
                + scope.toString().replace(".", UNDERSCORE)
                + CONTEXT_INSTANCES_SUFFIX;
        scopeToGeneratedName.put(scope, generatedName);
    }

    Collection<Resource> generate(DotName scope) {
        List<BeanInfo> beans = new BeanStream(beanDeployment.getBeans()).withScope(scope).collect();
        ResourceClassOutput classOutput = new ResourceClassOutput(true, generateSources);
        String generatedName = scopeToGeneratedName.get(scope);
        reflectionRegistration.registerMethod(generatedName, MethodDescriptor.INIT);

        ClassCreator contextInstances = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ContextInstances.class).build();

        // Add ContextInstanceHandle and Lock fields for every bean
        // The name of the field is a generated index
        // For example:
        // private volatile ContextInstanceHandle 1;
        // private final Lock 1l = new ReentrantLock();
        Map<String, InstanceAndLock> idToFields = new HashMap<>();
        int fieldIndex = 0;
        for (BeanInfo bean : beans) {
            String beanIdx = "" + fieldIndex++;
            FieldCreator handleField = contextInstances.getFieldCreator(beanIdx, ContextInstanceHandle.class)
                    .setModifiers(ACC_PRIVATE | ACC_VOLATILE);
            FieldCreator lockField = contextInstances.getFieldCreator(beanIdx + "l", Lock.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
            idToFields.put(bean.getIdentifier(),
                    new InstanceAndLock(handleField.getFieldDescriptor(), lockField.getFieldDescriptor()));
        }

        MethodCreator constructor = contextInstances.getMethodCreator(MethodDescriptor.INIT, "V");
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        for (InstanceAndLock fields : idToFields.values()) {
            constructor.writeInstanceField(fields.lock, constructor.getThis(),
                    constructor.newInstance(MethodDescriptor.ofConstructor(ReentrantLock.class)));
        }
        constructor.returnVoid();

        implementComputeIfAbsent(contextInstances, beans, idToFields);
        implementGetIfPresent(contextInstances, beans, idToFields);
        implementRemove(contextInstances, beans, idToFields);
        implementGetAllPresent(contextInstances, idToFields);
        implementRemoveEach(contextInstances, idToFields);

        // These methods are needed to significantly reduce the size of the stack map table for getAllPresent() and removeEach()
        implementLockAll(contextInstances, idToFields);
        implementUnlockAll(contextInstances, idToFields);

        contextInstances.close();

        return classOutput.getResources();
    }

    private void implementGetAllPresent(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        MethodCreator getAllPresent = contextInstances.getMethodCreator("getAllPresent", Set.class)
                .setModifiers(ACC_PUBLIC);
        // this.lockAll();
        // ContextInstanceHandle<?> copy1 = this.1;
        // this.unlockAll();
        // Set<ContextInstanceHandle<?>> ret = new HashSet<>();
        // if (copy1 != null) {
        //    ret.add(copy1);
        // }
        // return ret;
        getAllPresent.invokeVirtualMethod(MethodDescriptor.ofMethod(contextInstances.getClassName(), "lockAll", void.class),
                getAllPresent.getThis());
        List<ResultHandle> results = new ArrayList<>(idToFields.size());
        for (InstanceAndLock fields : idToFields.values()) {
            results.add(getAllPresent.readInstanceField(fields.instance, getAllPresent.getThis()));
        }
        getAllPresent.invokeVirtualMethod(MethodDescriptor.ofMethod(contextInstances.getClassName(), "unlockAll", void.class),
                getAllPresent.getThis());
        ResultHandle ret = getAllPresent.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (ResultHandle result : results) {
            getAllPresent.ifNotNull(result).trueBranch().invokeInterfaceMethod(MethodDescriptors.SET_ADD, ret, result);
        }
        getAllPresent.returnValue(ret);
    }

    private void implementLockAll(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        MethodCreator lockAll = contextInstances.getMethodCreator("lockAll", void.class)
                .setModifiers(ACC_PRIVATE);
        for (InstanceAndLock fields : idToFields.values()) {
            ResultHandle lock = lockAll.readInstanceField(fields.lock, lockAll.getThis());
            lockAll.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
        }
        lockAll.returnVoid();
    }

    private void implementUnlockAll(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        MethodCreator unlockAll = contextInstances.getMethodCreator("unlockAll", void.class)
                .setModifiers(ACC_PRIVATE);
        for (InstanceAndLock fields : idToFields.values()) {
            ResultHandle lock = unlockAll.readInstanceField(fields.lock, unlockAll.getThis());
            unlockAll.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        }
        unlockAll.returnVoid();
    }

    private void implementRemoveEach(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        MethodCreator removeEach = contextInstances.getMethodCreator("removeEach", void.class, Consumer.class)
                .setModifiers(ACC_PUBLIC);
        // this.lockAll();
        // ContextInstanceHandle<?> copy1 = this.1;
        // if (copy1 != null) {
        //    this.1 = null;
        // }
        // this.unlockAll();
        // if (action != null)
        //    if (copy1 != null) {
        //       consumer.accept(copy1);
        //    }
        // }
        removeEach.invokeVirtualMethod(MethodDescriptor.ofMethod(contextInstances.getClassName(), "lockAll", void.class),
                removeEach.getThis());
        List<ResultHandle> results = new ArrayList<>(idToFields.size());
        for (InstanceAndLock fields : idToFields.values()) {
            ResultHandle copy = removeEach.readInstanceField(fields.instance, removeEach.getThis());
            results.add(copy);
            BytecodeCreator isNotNull = removeEach.ifNotNull(copy).trueBranch();
            isNotNull.writeInstanceField(fields.instance, isNotNull.getThis(), isNotNull.loadNull());
        }
        removeEach.invokeVirtualMethod(MethodDescriptor.ofMethod(contextInstances.getClassName(), "unlockAll", void.class),
                removeEach.getThis());
        BytecodeCreator actionIsNotNull = removeEach.ifNotNull(removeEach.getMethodParam(0)).trueBranch();
        for (ResultHandle result : results) {
            BytecodeCreator isNotNull = actionIsNotNull.ifNotNull(result).trueBranch();
            isNotNull.invokeInterfaceMethod(MethodDescriptors.CONSUMER_ACCEPT, removeEach.getMethodParam(0), result);
        }
        removeEach.returnVoid();
    }

    private void implementRemove(ClassCreator contextInstances, List<BeanInfo> beans,
            Map<String, InstanceAndLock> idToFields) {
        MethodCreator remove = contextInstances
                .getMethodCreator("remove", ContextInstanceHandle.class, String.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = remove.stringSwitch(remove.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : beans) {
            InstanceAndLock fields = idToFields.get(bean.getIdentifier());
            FieldDescriptor instanceField = fields.instance;
            // There is a separate remove method for every instance handle field
            // To eliminate large stack map table in the bytecode
            MethodCreator removeHandle = contextInstances.getMethodCreator("r" + instanceField.getName(),
                    ContextInstanceHandle.class).setModifiers(ACC_PRIVATE);
            // this.1l.lock();
            // ContextInstanceHandle<?> copy = this.1;
            // if (copy != null) {
            //    this.1 = null;
            // }
            // this.1l.unlock();
            // return copy;
            ResultHandle lock = removeHandle.readInstanceField(fields.lock, removeHandle.getThis());
            removeHandle.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            ResultHandle copy = removeHandle.readInstanceField(instanceField, removeHandle.getThis());
            BytecodeCreator isNotNull = removeHandle.ifNotNull(copy).trueBranch();
            isNotNull.writeInstanceField(instanceField, isNotNull.getThis(), isNotNull.loadNull());
            removeHandle.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            removeHandle.returnValue(copy);

            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(removeHandle.getMethodDescriptor(), bc.getThis()));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    private void implementGetIfPresent(ClassCreator contextInstances, List<BeanInfo> beans,
            Map<String, InstanceAndLock> idToFields) {
        MethodCreator getIfPresent = contextInstances
                .getMethodCreator("getIfPresent", ContextInstanceHandle.class, String.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = getIfPresent.stringSwitch(getIfPresent.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : beans) {
            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.readInstanceField(idToFields.get(bean.getIdentifier()).instance, bc.getThis()));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    private void implementComputeIfAbsent(ClassCreator contextInstances, List<BeanInfo> beans,
            Map<String, InstanceAndLock> idToFields) {
        MethodCreator computeIfAbsent = contextInstances
                .getMethodCreator("computeIfAbsent", ContextInstanceHandle.class, String.class, Supplier.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = computeIfAbsent.stringSwitch(computeIfAbsent.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : beans) {
            InstanceAndLock fields = idToFields.get(bean.getIdentifier());
            // There is a separate compute method for every bean instance field
            MethodCreator compute = contextInstances.getMethodCreator("c" + fields.instance.getName(),
                    ContextInstanceHandle.class, Supplier.class).setModifiers(ACC_PRIVATE);
            // ContextInstanceHandle<?> copy = this.1;
            // if (copy != null) {
            //    return copy;
            // }
            // this.1l.lock();
            // try {
            //    if (this.1 == null) {
            //       this.1 = supplier.get();
            //    }
            //    this.1l.unlock();
            //    return this.1;
            // } catch(Throwable t) {
            //    this.1l.unlock();
            //    throw t;
            // }
            ResultHandle copy = compute.readInstanceField(fields.instance, compute.getThis());
            compute.ifNotNull(copy).trueBranch().returnValue(copy);
            ResultHandle lock = compute.readInstanceField(fields.lock, compute.getThis());
            compute.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            TryBlock tryBlock = compute.tryBlock();
            ResultHandle val = tryBlock.readInstanceField(fields.instance, compute.getThis());
            BytecodeCreator isNull = tryBlock.ifNull(val).trueBranch();
            ResultHandle newVal = isNull.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                    compute.getMethodParam(0));
            isNull.writeInstanceField(fields.instance, compute.getThis(), newVal);
            tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
            catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            catchBlock.throwException(catchBlock.getCaughtException());
            compute.returnValue(compute.readInstanceField(fields.instance, compute.getThis()));

            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(compute.getMethodDescriptor(), bc.getThis(), bc.getMethodParam(1)));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    record InstanceAndLock(FieldDescriptor instance, FieldDescriptor lock) {
    }

}
