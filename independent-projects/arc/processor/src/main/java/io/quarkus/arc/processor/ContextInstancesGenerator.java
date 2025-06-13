package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Reproducibility.orderedBeans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.jandex.DotName;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.impl.ContextInstances;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

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
        ResourceClassOutput classOutput = new ResourceClassOutput(true, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createContextInstances(gizmo, scope);

        return classOutput.getResources();
    }

    private void createContextInstances(Gizmo gizmo, DotName scope) {
        String generatedName = scopeToGeneratedName.get(scope);
        reflectionRegistration.registerMethod(generatedName, Methods.INIT);

        List<BeanInfo> beans = new BeanStream(beanDeployment.getBeans()).withScope(scope).collect();

        MethodDesc newUpdater = MethodDesc.of(AtomicReferenceFieldUpdater.class, "newUpdater",
                AtomicReferenceFieldUpdater.class, Class.class, Class.class, String.class);

        gizmo.class_(generatedName, cc -> {
            cc.implements_(ContextInstances.class);

            Map<String, BeanFields> beanFields = new TreeMap<>();
            int fieldIndex = 0;
            // We need to iterate the beans in order for the field names to be deterministic
            for (BeanInfo bean : orderedBeans(beans)) {
                String beanIdx = "" + fieldIndex++;

                // add these fields for each bean:
                // - `private volatile ContextInstanceHandle h<idx>`
                // - `private volatile Lock l<idx>`
                // - `private static final AtomicReferenceFieldUpdater<ContextInstances, ContextInstanceHandle> L<idx>_UPDATER`
                FieldDesc handleField = cc.field("h" + beanIdx, fc -> {
                    fc.private_();
                    fc.volatile_();
                    fc.setType(ContextInstanceHandle.class);
                });
                FieldDesc lockField = cc.field("l" + beanIdx, fc -> {
                    fc.private_();
                    fc.volatile_();
                    fc.setType(Lock.class);
                });
                StaticFieldVar lockUpdaterField = cc.staticField("L" + beanIdx + "_UPDATER", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(AtomicReferenceFieldUpdater.class);
                    fc.setInitializer(bc -> {
                        bc.yield(bc.invokeStatic(newUpdater, Const.of(cc.type()), Const.of(Lock.class),
                                Const.of("l" + beanIdx)));
                    });
                });

                beanFields.put(bean.getIdentifier(), new BeanFields(handleField, lockField, lockUpdaterField));
            }

            cc.defaultConstructor();

            Map<String, MethodDesc> lazyLocks = generateLazyLocks(cc, beanFields);
            generateComputeIfAbsent(cc, beanFields, lazyLocks);
            generateGetIfPresent(cc, beanFields);
            generateGetAllPresent(cc, beanFields);
            List<MethodDesc> remove = generateRemove(cc, beanFields, lazyLocks);
            generateRemoveEach(cc, remove);
        });
    }

    private Map<String, MethodDesc> generateLazyLocks(ClassCreator cc, Map<String, BeanFields> beanFields) {
        MethodDesc updaterCas = MethodDesc.of(AtomicReferenceFieldUpdater.class, "compareAndSet",
                boolean.class, Object.class, Object.class, Object.class);

        Map<String, MethodDesc> result = new HashMap<>(beanFields.size());
        for (Map.Entry<String, BeanFields> namedFields : beanFields.entrySet()) {
            String beanId = namedFields.getKey();
            BeanFields fields = namedFields.getValue();
            // private Lock lazyl<idx>() {
            //   if (this.l<idx> != null) {
            //     return this.l<idx>;
            //   }
            //   Lock newLock = new ReentrantLock();
            //   if (L<idx>_UPDATER.compareAndSet(this, null, newLock)) {
            //       return newLock;
            //   }
            //   return this.l<idx>;
            // }
            MethodDesc desc = cc.method("lazy" + fields.lock().name(), mc -> {
                mc.private_();
                mc.returning(Lock.class);
                mc.body(b0 -> {
                    FieldVar lock = cc.this_().field(fields.lock());
                    b0.ifNotNull(lock, b1 -> {
                        b1.return_(lock);
                    });
                    LocalVar newLock = b0.localVar("newLock", b0.new_(ReentrantLock.class));
                    Expr casResult = b0.invokeVirtual(updaterCas, fields.lockUpdater(),
                            cc.this_(), Const.ofNull(Lock.class), newLock);
                    b0.if_(casResult, b1 -> {
                        b1.return_(newLock);
                    });
                    b0.return_(lock);
                });
            });
            result.put(beanId, desc);
        }
        return result;
    }

    private void generateComputeIfAbsent(ClassCreator cc, Map<String, BeanFields> beanFields,
            Map<String, MethodDesc> lazyLocks) {
        Map<String, MethodDesc> computeMethodsByBean = new HashMap<>();
        for (Map.Entry<String, BeanFields> idToFields : beanFields.entrySet()) {
            String beanId = idToFields.getKey();
            BeanFields fields = idToFields.getValue();
            // There is a separate compute method for every bean instance field
            MethodDesc desc = cc.method("c" + fields.instance().name(), mc -> {
                mc.returning(ContextInstanceHandle.class);
                ParamVar supplier = mc.parameter("supplier", Supplier.class);
                mc.body(b0 -> {
                    // ContextInstanceHandle<?> copy = this.h<idx>;
                    // if (copy != null) {
                    //    return copy;
                    // }
                    // Lock lock = lazyl<idx>();
                    // lock.lock();
                    // try {
                    //   copy = this.h<idx>;
                    //   if (copy != null) {
                    //      return copy;
                    //   }
                    //   copy = supplier.get();
                    //   this.h<idx> = copy;
                    //   return copy;
                    // } finally {
                    //   lock.unlock();
                    // }
                    LocalVar copy = b0.localVar("copy", cc.this_().field(fields.instance()));
                    b0.ifNotNull(copy, b1 -> {
                        b1.return_(copy);
                    });
                    LocalVar lock = b0.localVar("lock", b0.invokeVirtual(lazyLocks.get(beanId), cc.this_()));
                    b0.locked(lock, b1 -> {
                        b1.set(copy, cc.this_().field(fields.instance()));
                        b1.ifNotNull(copy, b2 -> {
                            b2.return_(copy);
                        });
                        b1.set(copy, b1.invokeInterface(MethodDescs.SUPPLIER_GET, supplier));
                        b1.set(cc.this_().field(fields.instance()), copy);
                        b1.return_(copy);
                    });
                });
            });
            computeMethodsByBean.put(beanId, desc);
        }

        cc.method("computeIfAbsent", mc -> {
            mc.returning(ContextInstanceHandle.class);
            ParamVar rtBeanId = mc.parameter("beanId", String.class);
            ParamVar supplier = mc.parameter("supplier", Supplier.class);
            mc.body(b0 -> {
                b0.return_(b0.switch_(ContextInstanceHandle.class, rtBeanId, sc -> {
                    for (String btBeanId : beanFields.keySet()) {
                        sc.caseOf(btBeanId, b1 -> {
                            b1.return_(b1.invokeVirtual(computeMethodsByBean.get(btBeanId), cc.this_(), supplier));
                        });
                    }
                    sc.default_(b1 -> {
                        b1.throw_(IllegalArgumentException.class, "Unknown bean identifier");
                    });
                }));
            });
        });
    }

    private void generateGetIfPresent(ClassCreator cc, Map<String, BeanFields> beanFields) {
        cc.method("getIfPresent", mc -> {
            mc.returning(ContextInstanceHandle.class);
            ParamVar rtBeanId = mc.parameter("beanId", String.class);
            mc.body(b0 -> {
                b0.return_(b0.switch_(ContextInstanceHandle.class, rtBeanId, sc -> {
                    for (Map.Entry<String, BeanFields> idToFields : beanFields.entrySet()) {
                        String btBeanId = idToFields.getKey();
                        BeanFields fields = idToFields.getValue();
                        sc.caseOf(btBeanId, b1 -> {
                            b1.yield(cc.this_().field(fields.instance()));
                        });
                    }
                    sc.default_(b1 -> {
                        b1.throw_(IllegalArgumentException.class, "Unknown bean identifier");
                    });
                }));
            });
        });
    }

    private void generateGetAllPresent(ClassCreator cc, Map<String, BeanFields> beanFields) {
        cc.method("getAllPresent", mc -> {
            mc.returning(Set.class);
            mc.body(b0 -> {
                // ContextInstanceHandle<?> h<idx> = this.h<idx>;
                // Set<ContextInstanceHandle<?>> result = new HashSet<>();
                // if (h<idx> != null) {
                //    result.add(h<idx>);
                // }
                // return result;
                List<LocalVar> handles = new ArrayList<>(beanFields.size());
                for (BeanFields fields : beanFields.values()) {
                    handles.add(b0.localVar(cc.this_().field(fields.instance)));
                }
                LocalVar result = b0.localVar("result", b0.new_(HashSet.class));
                for (LocalVar handle : handles) {
                    b0.ifNotNull(handle, b1 -> {
                        b1.withSet(result).add(handle);
                    });
                }
                b0.return_(result);
            });
        });
    }

    private List<MethodDesc> generateRemove(ClassCreator cc, Map<String, BeanFields> beanFields,
            Map<String, MethodDesc> lazyLocks) {

        // There is a separate remove method for every instance handle field
        // To eliminate large stack map table in the bytecode
        List<MethodDesc> removeMethods = new ArrayList<>(beanFields.size());
        Map<String, MethodDesc> removeMethodsByBean = new HashMap<>();
        for (Map.Entry<String, BeanFields> idToFields : beanFields.entrySet()) {
            String beanId = idToFields.getKey();
            BeanFields fields = idToFields.getValue();
            FieldDesc instanceField = fields.instance;
            MethodDesc desc = cc.method("r" + instanceField.name(), mc -> {
                mc.returning(ContextInstanceHandle.class);
                mc.body(b0 -> {
                    // ContextInstanceHandle<?> copy = this.h<idx>;
                    // if (copy == null) {
                    //   return null;
                    // }
                    // Lock lock = lazyl<idx>();
                    // lock.lock();
                    // try {
                    //   copy = this.h<idx>;
                    //   this.h<idx> = null;
                    // } finally {
                    //   lock.unlock();
                    // }
                    // return copy;
                    LocalVar copy = b0.localVar("copy", cc.this_().field(instanceField));
                    b0.ifNull(copy, b1 -> {
                        b1.return_(Const.ofNull(ContextInstanceHandle.class));
                    });
                    LocalVar lock = b0.localVar("lock", b0.invokeVirtual(lazyLocks.get(beanId), cc.this_()));
                    b0.locked(lock, b1 -> {
                        b1.set(copy, cc.this_().field(instanceField));
                        b1.set(cc.this_().field(instanceField), Const.ofNull(ContextInstanceHandle.class));
                    });
                    b0.return_(copy);
                });
            });
            removeMethods.add(desc);
            removeMethodsByBean.put(beanId, desc);
        }

        cc.method("remove", mc -> {
            mc.returning(ContextInstanceHandle.class);
            ParamVar rtBeanId = mc.parameter("beanId", String.class);
            mc.body(b0 -> {
                b0.return_(b0.switch_(ContextInstanceHandle.class, rtBeanId, sc -> {
                    for (String btBeanId : beanFields.keySet()) {
                        sc.caseOf(btBeanId, b1 -> {
                            b1.return_(b1.invokeVirtual(removeMethodsByBean.get(btBeanId), cc.this_()));
                        });
                    }
                    sc.default_(b1 -> {
                        b1.throw_(IllegalArgumentException.class, "Unknown bean identifier");
                    });
                }));
            });
        });

        return removeMethods;
    }

    private void generateRemoveEach(ClassCreator cc, List<MethodDesc> removeInstances) {
        cc.method("removeEach", mc -> {
            mc.returning(void.class);
            ParamVar action = mc.parameter("action", Consumer.class);
            mc.body(b0 -> {
                // ContextInstanceHandle<?> copy<idx> = rh<idx>();
                // if (action != null)
                //    if (copy<idx> != null) {
                //       action.accept(copy<idx>);
                //    }
                // }
                int counter = 0;
                List<LocalVar> results = new ArrayList<>(removeInstances.size());
                for (MethodDesc removeInstance : removeInstances) {
                    // invoke remove method for every instance handle field
                    results.add(b0.localVar("copy" + counter, b0.invokeVirtual(removeInstance, cc.this_())));
                    counter++;
                }
                b0.ifNotNull(action, b1 -> {
                    for (LocalVar result : results) {
                        b1.ifNotNull(result, b2 -> {
                            b2.invokeInterface(MethodDescs.CONSUMER_ACCEPT, action, result);
                        });
                    }
                });
                b0.return_();
            });
        });
    }

    record BeanFields(FieldDesc instance, FieldDesc lock, StaticFieldVar lockUpdater) {
    }
}
