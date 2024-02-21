package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
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
        // private static final AtomicReferenceFieldUpdater<ContextInstances, ContextInstanceHandle> LAZY_1L_UPDATER;
        // private volatile ContextInstanceHandle 1;
        // private volatile Lock 1l;
        Map<String, InstanceAndLock> idToFields = new HashMap<>();
        int fieldIndex = 0;
        for (BeanInfo bean : beans) {
            String beanIdx = "" + fieldIndex++;
            FieldCreator handleField = contextInstances.getFieldCreator(beanIdx, ContextInstanceHandle.class)
                    .setModifiers(ACC_PRIVATE | ACC_VOLATILE);
            FieldCreator lockField = contextInstances.getFieldCreator(beanIdx + "l", Lock.class)
                    .setModifiers(ACC_PRIVATE | ACC_VOLATILE);
            FieldCreator atomicLockField = contextInstances.getFieldCreator("LAZY_" + beanIdx + "L_UPDATER",
                    AtomicReferenceFieldUpdater.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL | ACC_STATIC);
            idToFields.put(bean.getIdentifier(),
                    new InstanceAndLock(handleField.getFieldDescriptor(), lockField.getFieldDescriptor(),
                            atomicLockField.getFieldDescriptor()));
        }

        implementStaticConstructor(contextInstances, idToFields);
        Map<String, MethodDescriptor> lazyLocks = implementLazyLocks(contextInstances, idToFields);

        MethodCreator constructor = contextInstances.getMethodCreator(MethodDescriptor.INIT, "V");
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        constructor.returnVoid();

        implementComputeIfAbsent(contextInstances, beans, idToFields, lazyLocks);
        implementGetIfPresent(contextInstances, beans, idToFields);
        List<MethodDescriptor> remove = implementRemove(contextInstances, beans, idToFields, lazyLocks);
        implementGetAllPresent(contextInstances, idToFields);
        implementRemoveEach(contextInstances, remove);

        contextInstances.close();

        return classOutput.getResources();
    }

    private static void implementStaticConstructor(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        // Add a static initializer to initialize the AtomicReferenceFieldUpdater fields
        // static {
        //   LAZY_1L_UPDATER = AtomicReferenceFieldUpdater.newUpdater(ContextInstances.class, Lock.class, "1l");
        // }
        MethodCreator staticConstructor = contextInstances.getMethodCreator(MethodDescriptor.CLINIT, void.class)
                .setModifiers(ACC_STATIC);

        MethodDescriptor newLockUpdater = MethodDescriptor.ofMethod(AtomicReferenceFieldUpdater.class, "newUpdater",
                AtomicReferenceFieldUpdater.class, Class.class, Class.class, String.class);

        for (InstanceAndLock fields : idToFields.values()) {
            ResultHandle updater = staticConstructor.invokeStaticMethod(newLockUpdater,
                    staticConstructor.loadClass(contextInstances.getClassName()),
                    staticConstructor.loadClass(Lock.class),
                    staticConstructor.load(fields.lock.getName()));
            staticConstructor.writeStaticField(fields.lockUpdater, updater);
        }
        staticConstructor.returnVoid();
    }

    private static Map<String, MethodDescriptor> implementLazyLocks(ClassCreator contextInstances,
            Map<String, InstanceAndLock> idToFields) {
        MethodDescriptor atomicReferenceFieldUpdaterCompareAndSet = MethodDescriptor.ofMethod(
                AtomicReferenceFieldUpdater.class, "compareAndSet",
                boolean.class, Object.class, Object.class, Object.class);
        // generated lazy lock initializers methods
        // private Lock lazy1l() {
        //   Lock lock = this.1l;
        //   if (lock != null) {
        //     return lock;
        //   }
        //   lock = new ReentrantLock();
        //   if (LAZY_1L_UPDATER.compareAndSet(this, null, lock)) {
        //       return lock;
        //   }
        //   return this.1l;
        // }
        Map<String, MethodDescriptor> lazyLockMethods = new HashMap<>(idToFields.size());
        for (var namedFields : idToFields.entrySet()) {
            var fields = namedFields.getValue();
            MethodCreator lazyLockMethod = contextInstances.getMethodCreator("lazy" + fields.lock.getName(), Lock.class)
                    .setModifiers(ACC_PRIVATE);
            ResultHandle lock = lazyLockMethod.readInstanceField(fields.lock, lazyLockMethod.getThis());
            lazyLockMethod
                    .ifNotNull(lock).trueBranch()
                    .returnValue(lock);
            ResultHandle newLock = lazyLockMethod.newInstance(MethodDescriptor.ofConstructor(ReentrantLock.class));
            ResultHandle updated = lazyLockMethod.invokeVirtualMethod(atomicReferenceFieldUpdaterCompareAndSet,
                    lazyLockMethod.readStaticField(fields.lockUpdater), lazyLockMethod.getThis(),
                    lazyLockMethod.loadNull(), newLock);
            lazyLockMethod
                    .ifTrue(updated).trueBranch()
                    .returnValue(newLock);
            lazyLockMethod.returnValue(lazyLockMethod.readInstanceField(fields.lock, lazyLockMethod.getThis()));
            lazyLockMethods.put(namedFields.getKey(), lazyLockMethod.getMethodDescriptor());
        }
        return lazyLockMethods;
    }

    private void implementGetAllPresent(ClassCreator contextInstances, Map<String, InstanceAndLock> idToFields) {
        MethodCreator getAllPresent = contextInstances.getMethodCreator("getAllPresent", Set.class)
                .setModifiers(ACC_PUBLIC);
        // ContextInstanceHandle<?> copy1 = this.1;
        // Set<ContextInstanceHandle<?>> ret = new HashSet<>();
        // if (copy1 != null) {
        //    ret.add(copy1);
        // }
        // return ret;
        List<ResultHandle> results = new ArrayList<>(idToFields.size());
        for (InstanceAndLock fields : idToFields.values()) {
            results.add(getAllPresent.readInstanceField(fields.instance, getAllPresent.getThis()));
        }
        ResultHandle ret = getAllPresent.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (ResultHandle result : results) {
            getAllPresent.ifNotNull(result).trueBranch().invokeInterfaceMethod(MethodDescriptors.SET_ADD, ret, result);
        }
        getAllPresent.returnValue(ret);
    }

    private void implementRemoveEach(ClassCreator contextInstances, List<MethodDescriptor> removeInstances) {
        MethodCreator removeEach = contextInstances.getMethodCreator("removeEach", void.class, Consumer.class)
                .setModifiers(ACC_PUBLIC);
        // ContextInstanceHandle<?> copy1 = remove1();
        // if (action != null)
        //    if (copy1 != null) {
        //       consumer.accept(copy1);
        //    }
        // }

        List<ResultHandle> results = new ArrayList<>(removeInstances.size());
        for (MethodDescriptor removeInstance : removeInstances) {
            // invoke remove method for every instance handle field
            results.add(removeEach.invokeVirtualMethod(removeInstance, removeEach.getThis()));
        }
        BytecodeCreator actionIsNotNull = removeEach.ifNotNull(removeEach.getMethodParam(0)).trueBranch();
        for (ResultHandle result : results) {
            BytecodeCreator isNotNull = actionIsNotNull.ifNotNull(result).trueBranch();
            isNotNull.invokeInterfaceMethod(MethodDescriptors.CONSUMER_ACCEPT, removeEach.getMethodParam(0), result);
        }
        removeEach.returnVoid();
    }

    private List<MethodDescriptor> implementRemove(ClassCreator contextInstances, List<BeanInfo> beans,
            Map<String, InstanceAndLock> idToFields, Map<String, MethodDescriptor> lazyLocks) {
        MethodCreator remove = contextInstances
                .getMethodCreator("remove", ContextInstanceHandle.class, String.class)
                .setModifiers(ACC_PUBLIC);

        StringSwitch strSwitch = remove.stringSwitch(remove.getMethodParam(0));
        // https://github.com/quarkusio/gizmo/issues/164
        strSwitch.fallThrough();
        List<MethodDescriptor> removeMethods = new ArrayList<>(beans.size());
        for (BeanInfo bean : beans) {
            InstanceAndLock fields = idToFields.get(bean.getIdentifier());
            FieldDescriptor instanceField = fields.instance;
            // There is a separate remove method for every instance handle field
            // To eliminate large stack map table in the bytecode
            MethodCreator removeHandle = contextInstances.getMethodCreator("r" + instanceField.getName(),
                    ContextInstanceHandle.class).setModifiers(ACC_PRIVATE);
            // ContextInstanceHandle<?> copy = this.1;
            // if (copy == null) {
            //  return null;
            // }
            // Lock lock = lazy1l();
            // lock.lock();
            // copy = this.1;
            // this.1 = null;
            // lock.unlock();
            // return copy;
            ResultHandle copy = removeHandle.readInstanceField(instanceField, removeHandle.getThis());
            removeHandle.ifNull(copy).trueBranch()
                    .returnValue(removeHandle.loadNull());
            ResultHandle lock = removeHandle.invokeVirtualMethod(lazyLocks.get(bean.getIdentifier()), removeHandle.getThis());
            removeHandle.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            copy = removeHandle.readInstanceField(instanceField, removeHandle.getThis());
            removeHandle.writeInstanceField(instanceField, removeHandle.getThis(), removeHandle.loadNull());
            removeHandle.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            removeHandle.returnValue(copy);
            removeMethods.add(removeHandle.getMethodDescriptor());
            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(removeHandle.getMethodDescriptor(), bc.getThis()));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
        return removeMethods;
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
            Map<String, InstanceAndLock> idToFields, Map<String, MethodDescriptor> lazyLocks) {
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
            // Lock lock = lazy1l();
            // lock.lock();
            // copy = this.1;
            // if (copy != null) {
            //    lock.unlock();
            //    return copy;

            // try {
            //    copy = supplier.get();
            //    this.1 = copy;
            //    lock.unlock();
            //    return copy;
            // } catch(Throwable t) {
            //    lock.unlock();
            //    throw t;
            // }
            ResultHandle copy = compute.readInstanceField(fields.instance, compute.getThis());
            compute.ifNotNull(copy).trueBranch().returnValue(copy);
            ResultHandle lock = compute.invokeVirtualMethod(lazyLocks.get(bean.getIdentifier()), compute.getThis());
            compute.invokeInterfaceMethod(MethodDescriptors.LOCK_LOCK, lock);
            copy = compute.readInstanceField(fields.instance, compute.getThis());
            BytecodeCreator nonNullCopy = compute.ifNotNull(copy).trueBranch();
            nonNullCopy.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            nonNullCopy.returnValue(copy);
            TryBlock tryBlock = compute.tryBlock();
            copy = tryBlock.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET, compute.getMethodParam(0));
            tryBlock.writeInstanceField(fields.instance, compute.getThis(), copy);
            tryBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            tryBlock.returnValue(copy);
            CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
            catchBlock.invokeInterfaceMethod(MethodDescriptors.LOCK_UNLOCK, lock);
            catchBlock.throwException(catchBlock.getCaughtException());

            strSwitch.caseOf(bean.getIdentifier(), bc -> {
                bc.returnValue(bc.invokeVirtualMethod(compute.getMethodDescriptor(), bc.getThis(), bc.getMethodParam(1)));
            });
        }
        strSwitch.defaultCase(bc -> bc.throwException(IllegalArgumentException.class, "Unknown bean identifier"));
    }

    record InstanceAndLock(FieldDescriptor instance, FieldDescriptor lock, FieldDescriptor lockUpdater) {
    }

}
