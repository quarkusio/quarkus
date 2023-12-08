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

    static final String APP_CONTEXT_INSTANCES_SUFFIX = "_ContextInstances";

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
                + APP_CONTEXT_INSTANCES_SUFFIX;
        scopeToGeneratedName.put(scope, generatedName);
    }

    Collection<Resource> generate(DotName scope) {
        List<BeanInfo> beans = new BeanStream(beanDeployment.getBeans()).withScope(scope).collect();
        ResourceClassOutput classOutput = new ResourceClassOutput(true, generateSources);
        String generatedName = scopeToGeneratedName.get(scope);
        reflectionRegistration.registerMethod(generatedName, MethodDescriptor.INIT);

        ClassCreator contextInstances = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ContextInstances.class).build();

        // Add fields for all beans
        // The name of the field is a generated index
        // For example:
        // private volatile ContextInstanceHandle 1;
        Map<String, FieldDescriptor> idToField = new HashMap<>();
        int fieldIndex = 0;
        for (BeanInfo bean : beans) {
            FieldCreator fc = contextInstances.getFieldCreator("" + fieldIndex++, ContextInstanceHandle.class)
                    .setModifiers(ACC_PRIVATE | ACC_VOLATILE);
            idToField.put(bean.getIdentifier(), fc.getFieldDescriptor());
        }

        FieldCreator lockField = contextInstances.getFieldCreator("lock", Lock.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        MethodCreator constructor = contextInstances.getMethodCreator(MethodDescriptor.INIT, "V");
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        constructor.writeInstanceField(lockField.getFieldDescriptor(), constructor.getThis(),
                constructor.newInstance(MethodDescriptor.ofConstructor(ReentrantLock.class)));
        constructor.returnVoid();

        implementComputeIfAbsent(contextInstances, beans, idToField,
                lockField.getFieldDescriptor());
        implementGetIfPresent(contextInstances, beans, idToField);
        implementRemove(contextInstances, beans, idToField, lockField.getFieldDescriptor());
        implementClear(contextInstances, idToField, lockField.getFieldDescriptor());
        implementGetAllPresent(contextInstances, idToField, lockField.getFieldDescriptor());
        implementForEach(contextInstances, idToField, lockField.getFieldDescriptor());
        contextInstances.close();

        return classOutput.getResources();
    }

    private void implementGetAllPresent(ClassCreator contextInstances, Map<String, FieldDescriptor> idToField,
            FieldDescriptor lockField) {
        MethodCreator getAllPresent = contextInstances.getMethodCreator("getAllPresent", Set.class)
                .setModifiers(ACC_PUBLIC);
        // lock.lock();
        // try {
        //    Set<ContextInstanceHandle<?>> ret = new HashSet<>();
        //    ContextInstanceHandle<?> copy = this.1;
        //    if (copy != null) {
        //       ret.add(copy);
        //    }
        //    return ret;
        // } catch(Throwable t) {
        //    lock.unlock();
        //    throw t;
        // }
        ResultHandle lock = getAllPresent.readInstanceField(lockField, getAllPresent.getThis());
        getAllPresent.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
        TryBlock tryBlock = getAllPresent.tryBlock();
        ResultHandle ret = tryBlock.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (FieldDescriptor field : idToField.values()) {
            ResultHandle copy = tryBlock.readInstanceField(field, tryBlock.getThis());
            tryBlock.ifNotNull(copy).trueBranch().invokeInterfaceMethod(MethodDescriptors.SET_ADD, ret, copy);
        }
        tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        tryBlock.returnValue(ret);
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        catchBlock.throwException(catchBlock.getCaughtException());
    }

    private void implementClear(ClassCreator applicationContextInstances, Map<String, FieldDescriptor> idToField,
            FieldDescriptor lockField) {
        MethodCreator clear = applicationContextInstances.getMethodCreator("clear", void.class).setModifiers(ACC_PUBLIC);
        // lock.lock();
        // try {
        //    this.1 = null;
        //    lock.unlock();
        // } catch(Throwable t) {
        //    lock.unlock();
        //    throw t;
        // }
        ResultHandle lock = clear.readInstanceField(lockField, clear.getThis());
        clear.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
        TryBlock tryBlock = clear.tryBlock();
        for (FieldDescriptor field : idToField.values()) {
            tryBlock.writeInstanceField(field, tryBlock.getThis(), tryBlock.loadNull());
        }
        tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        tryBlock.returnVoid();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        catchBlock.throwException(catchBlock.getCaughtException());
    }

    private void implementForEach(ClassCreator contextInstances, Map<String, FieldDescriptor> idToField,
            FieldDescriptor lockField) {
        MethodCreator forEach = contextInstances.getMethodCreator("forEach", void.class, Consumer.class)
                .setModifiers(ACC_PUBLIC);
        // lock.lock();
        // ContextInstanceHandle<?> copy = this.1;
        // lock.unlock();
        // if (copy != null) {
        //    consumer.accept(copy);
        // }
        ResultHandle lock = forEach.readInstanceField(lockField, forEach.getThis());
        forEach.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
        List<ResultHandle> results = new ArrayList<>(idToField.size());
        for (FieldDescriptor field : idToField.values()) {
            results.add(forEach.readInstanceField(field, forEach.getThis()));
        }
        forEach.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
        for (int i = 0; i < results.size(); i++) {
            ResultHandle copy = results.get(i);
            BytecodeCreator isNotNull = forEach.ifNotNull(copy).trueBranch();
            isNotNull.invokeInterfaceMethod(MethodDescriptors.CONSUMER_ACCEPT, forEach.getMethodParam(0), copy);
        }
        forEach.returnVoid();
    }

    private void implementRemove(ClassCreator contextInstances, List<BeanInfo> applicationScopedBeans,
            Map<String, FieldDescriptor> idToField, FieldDescriptor lockField) {
        MethodCreator remove = contextInstances
                .getMethodCreator("remove", ContextInstanceHandle.class, String.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = remove.stringSwitch(remove.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : applicationScopedBeans) {
            FieldDescriptor instanceField = idToField.get(bean.getIdentifier());
            // There is a separate remove method for every bean instance field
            MethodCreator removeBean = contextInstances.getMethodCreator("r" + instanceField.getName(),
                    ContextInstanceHandle.class).setModifiers(ACC_PRIVATE);
            // lock.lock();
            // try {
            //    ContextInstanceHandle<?> copy = this.1;
            //    if (copy != null) {
            //       this.1 = null;
            //    }
            //    lock.unlock();
            //    return copy;
            // } catch(Throwable t) {
            //    lock.unlock();
            //    throw t;
            // }

            ResultHandle lock = removeBean.readInstanceField(lockField, removeBean.getThis());
            removeBean.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            TryBlock tryBlock = removeBean.tryBlock();
            ResultHandle copy = tryBlock.readInstanceField(instanceField, tryBlock.getThis());
            BytecodeCreator isNotNull = tryBlock.ifNotNull(copy).trueBranch();
            isNotNull.writeInstanceField(instanceField, isNotNull.getThis(), isNotNull.loadNull());
            tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            tryBlock.returnValue(copy);
            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
            catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            catchBlock.throwException(catchBlock.getCaughtException());

            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(removeBean.getMethodDescriptor(), bc.getThis()));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    private void implementGetIfPresent(ClassCreator contextInstances, List<BeanInfo> applicationScopedBeans,
            Map<String, FieldDescriptor> idToField) {
        MethodCreator getIfPresent = contextInstances
                .getMethodCreator("getIfPresent", ContextInstanceHandle.class, String.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = getIfPresent.stringSwitch(getIfPresent.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : applicationScopedBeans) {
            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.readInstanceField(idToField.get(bean.getIdentifier()), bc.getThis()));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    private void implementComputeIfAbsent(ClassCreator contextInstances, List<BeanInfo> applicationScopedBeans,
            Map<String, FieldDescriptor> idToField, FieldDescriptor lockField) {
        MethodCreator computeIfAbsent = contextInstances
                .getMethodCreator("computeIfAbsent", ContextInstanceHandle.class, String.class, Supplier.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = computeIfAbsent.stringSwitch(computeIfAbsent.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        for (BeanInfo bean : applicationScopedBeans) {
            FieldDescriptor instanceField = idToField.get(bean.getIdentifier());
            // There is a separate compute method for every bean instance field
            MethodCreator compute = contextInstances.getMethodCreator("c" + instanceField.getName(),
                    ContextInstanceHandle.class, Supplier.class).setModifiers(ACC_PRIVATE);
            // ContextInstanceHandle<?> copy = this.1;
            // if (copy != null) {
            //    return copy;
            // }
            // lock.lock();
            // try {
            //    if (this.1 == null) {
            //       this.1 = supplier.get();
            //    }
            //    lock.unlock();
            //    return this.1;
            // } catch(Throwable t) {
            //    lock.unlock();
            //    throw t;
            // }
            ResultHandle copy = compute.readInstanceField(instanceField, compute.getThis());
            compute.ifNotNull(copy).trueBranch().returnValue(copy);
            ResultHandle lock = compute.readInstanceField(lockField, compute.getThis());
            compute.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            TryBlock tryBlock = compute.tryBlock();
            ResultHandle val = tryBlock.readInstanceField(instanceField, compute.getThis());
            BytecodeCreator isNull = tryBlock.ifNull(val).trueBranch();
            ResultHandle newVal = isNull.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                    compute.getMethodParam(0));
            isNull.writeInstanceField(instanceField, compute.getThis(), newVal);
            tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
            catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            catchBlock.throwException(catchBlock.getCaughtException());
            compute.returnValue(compute.readInstanceField(instanceField, compute.getThis()));

            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(compute.getMethodDescriptor(), bc.getThis(), bc.getMethodParam(1)));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

}
