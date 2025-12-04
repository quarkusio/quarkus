package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Reproducibility.orderedBeans;
import static io.quarkus.arc.processor.Reproducibility.orderedDecorators;
import static io.quarkus.arc.processor.Reproducibility.orderedInterceptors;
import static io.quarkus.arc.processor.Reproducibility.orderedObservers;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.constant.ClassDesc;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.smallrye.common.annotation.SuppressForbidden;

/**
 *
 * @author Martin Kouba
 */
// this must be on the entire class because of a single occurrence in `createAddRemovedBeansMethods()`,
// because it appears in a deeply nested lambda and Forbidden APIs fails to attribute it to the declaring method
@SuppressForbidden(reason = "Using Type.toString() to build an informative message")
public class ComponentsProviderGenerator extends AbstractGenerator {

    static final String COMPONENTS_PROVIDER_SUFFIX = "_ComponentsProvider";
    static final String SETUP_PACKAGE = Arc.class.getPackage().getName() + ".setup";
    static final String ADD_OBSERVERS = "addObservers";
    static final String ADD_REMOVED_BEANS = "addRemovedBeans";
    static final String ADD_BEANS = "addBeans";

    private static final int BEAN_GROUP_SIZE = 30;
    private static final int OBSERVER_GROUP_SIZE = 30;
    private static final int REMOVED_BEAN_GROUP_SIZE = 5;
    private static final int CONTAINER_SIZE = 10;

    private final AnnotationLiteralProcessor annotationLiterals;
    private final boolean detectUnusedFalsePositives;

    public ComponentsProviderGenerator(AnnotationLiteralProcessor annotationLiterals, boolean generateSources,
            boolean detectUnusedFalsePositives) {
        super(generateSources);
        this.annotationLiterals = annotationLiterals;
        this.detectUnusedFalsePositives = detectUnusedFalsePositives;
    }

    /**
     *
     * @param name
     * @param beanDeployment
     * @param beanToGeneratedName
     * @param observerToGeneratedName
     * @param scopeToContextInstances
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, Map<BeanInfo, String> beanToGeneratedName,
            Map<ObserverInfo, String> observerToGeneratedName, Map<DotName, String> scopeToContextInstances) {

        ResourceClassOutput classOutput = new ResourceClassOutput(true, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createComponentsProvider(gizmo, name, beanDeployment, beanToGeneratedName, observerToGeneratedName,
                scopeToContextInstances);

        List<Resource> resources = new ArrayList<>();
        for (Resource resource : classOutput.getResources()) {
            resources.add(resource);
            if (resource.getName().endsWith(COMPONENTS_PROVIDER_SUFFIX)) {
                // We need to filter out nested classes and functions
                resources.add(ResourceImpl.serviceProvider(ComponentsProvider.class.getName(),
                        resource.getName().replace('/', '.').getBytes(StandardCharsets.UTF_8), null));
            }
        }
        return resources;
    }

    private void createComponentsProvider(Gizmo gizmo, String name, BeanDeployment beanDeployment,
            Map<BeanInfo, String> beanToGeneratedName, Map<ObserverInfo, String> observerToGeneratedName,
            Map<DotName, String> scopeToContextInstances) {

        CodeGenInfo info = preprocess(beanDeployment);

        String generatedName = SETUP_PACKAGE + "." + name + COMPONENTS_PROVIDER_SUFFIX;

        gizmo.class_(generatedName, cc -> {
            cc.implements_(ComponentsProvider.class);

            cc.defaultConstructor();

            cc.method("getComponents", mc -> {
                mc.returning(Components.class);
                ParamVar currentContextFactory = mc.parameter("currentContextFactory", CurrentContextFactory.class);
                mc.body(bc -> {
                    // Break bean processing into multiple groups
                    // Each group is represented by a static method addBeansX(); X corresponds to the group id
                    // Several groups belong to a container - a class named like Foo_ComponentsProvider_addBeansY; Y corresponds to the container id

                    // Map<String, InjectableBean<?>>
                    LocalVar beanIdToBean = bc.localVar("beanIdToBean", bc.new_(HashMap.class));
                    for (Container<BeanGroup> container : info.beans()) {
                        for (BeanGroup group : container) {
                            ClassMethodDesc desc = ClassMethodDesc.of(
                                    ClassDesc.of(container.className(generatedName, ADD_BEANS)),
                                    ADD_BEANS + group.id(),
                                    void.class, Map.class);
                            bc.invokeStatic(desc, beanIdToBean);
                        }
                    }

                    LocalVar beans = bc.localVar("beans", bc.withMap(beanIdToBean).values());
                    generateAddBeans(generatedName, gizmo, info, beanToGeneratedName);

                    // Break observer processing into multiple groups
                    // Each group is represented by a static method addObserversX(); X corresponds to the group id
                    // Several groups belong to a container - a class named like Foo_ComponentsProvider_addObserversY; Y corresponds to the container id

                    // List<InjectableObserverMethod<?>
                    LocalVar observers = bc.localVar("observers", bc.new_(ArrayList.class));
                    for (Container<ObserverGroup> container : info.observers()) {
                        for (ObserverGroup group : container) {
                            ClassMethodDesc desc = ClassMethodDesc.of(
                                    ClassDesc.of(container.className(generatedName, ADD_OBSERVERS)),
                                    ADD_OBSERVERS + group.id(),
                                    void.class, Map.class, List.class);
                            bc.invokeStatic(desc, beanIdToBean, observers);
                        }
                    }
                    generateAddObservers(generatedName, gizmo, info, observerToGeneratedName);

                    // Custom contexts
                    // List<InjectableContext>
                    ContextConfigurator.CreateGeneration createGeneration = new ContextConfigurator.CreateGeneration() {
                        @Override
                        public BlockCreator method() {
                            return bc;
                        }

                        @Override
                        public Var currentContextFactory() {
                            return currentContextFactory;
                        }
                    };
                    LocalVar contexts = bc.localVar("contexts", bc.new_(ArrayList.class));
                    for (var creators : beanDeployment.getCustomContexts().values()) {
                        for (Function<ContextConfigurator.CreateGeneration, Expr> creator : creators) {
                            bc.withList(contexts).add(creator.apply(createGeneration));
                        }
                    }

                    // All interceptor bindings
                    // Set<String>
                    LocalVar interceptorBindings = bc.localVar("interceptorBindings", bc.new_(HashSet.class));
                    for (ClassInfo binding : beanDeployment.getInterceptorBindings()) {
                        bc.withSet(interceptorBindings).add(Const.of(binding.name().toString()));
                    }

                    // Transitive interceptor bindings
                    // Map<Class, Set<Annotation>>
                    LocalVar transitiveBindings = bc.localVar("transitiveBindings", bc.new_(HashMap.class));
                    beanDeployment.getTransitiveInterceptorBindings().forEach((binding, transitives) -> {
                        LocalVar transitivesSet = bc.localVar("transitives", bc.new_(HashSet.class));
                        for (AnnotationInstance transitive : transitives) {
                            ClassInfo transitiveClass = beanDeployment.getInterceptorBinding(transitive.name());
                            bc.withSet(transitivesSet).add(annotationLiterals.create(bc, transitiveClass, transitive));
                        }
                        bc.withMap(transitiveBindings).put(Const.of(classDescOf(binding)), transitivesSet);
                    });

                    // removed beans
                    // Supplier<Collection<RemovedBean>>
                    LocalVar removedBeansSupplier;
                    if (detectUnusedFalsePositives) {
                        removedBeansSupplier = bc.localVar("removedBeansSupplier", bc.lambda(Supplier.class, lc -> {
                            lc.body(lbc -> {
                                LocalVar removedBeans = lbc.localVar("removedBeans", lbc.new_(ArrayList.class));
                                LocalVar typeCache = lbc.localVar("typeCache", lbc.new_(HashMap.class));

                                // Break observer processing into multiple groups
                                // Each group is represented by a static method addRemovedBeansX(); X corresponds to the group id
                                // Several groups belong to a container - a class named like Foo_ComponentsProvider_addRemovedBeansY; Y corresponds to the container id
                                for (Container<RemovedBeanGroup> container : info.removedBeans()) {
                                    for (RemovedBeanGroup group : container) {
                                        ClassMethodDesc desc = ClassMethodDesc.of(
                                                ClassDesc.of(container.className(generatedName, ADD_REMOVED_BEANS)),
                                                ADD_REMOVED_BEANS + group.id(),
                                                void.class, List.class, Map.class);
                                        lbc.invokeStatic(desc, removedBeans, typeCache);
                                    }
                                }
                                lbc.return_(removedBeans);
                            });
                        }));
                        generateAddRemovedBeans(generatedName, gizmo, info);
                    } else {
                        removedBeansSupplier = bc.localVar("removedBeansSupplier",
                                bc.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, bc.setOf()));
                    }

                    // All qualifiers
                    // Set<String>
                    LocalVar qualifiers = bc.localVar("qualifiers", bc.new_(HashSet.class));
                    for (ClassInfo qualifier : beanDeployment.getQualifiers()) {
                        bc.withSet(qualifiers).add(Const.of(qualifier.name().toString()));
                    }

                    // Qualifier non-binding members
                    LocalVar qualifiersNonbindingMembers = bc.localVar("qualifiersNonbindingMembers", bc.new_(HashMap.class));
                    beanDeployment.getQualifierNonbindingMembers().forEach((qualifier, nonbindingMembers) -> {
                        LocalVar nonbindingMembersSet = bc.localVar("nonbindingMembers", bc.new_(HashSet.class));
                        for (String nonbindingMember : nonbindingMembers) {
                            bc.withSet(nonbindingMembersSet).add(Const.of(nonbindingMember));
                        }
                        bc.withMap(qualifiersNonbindingMembers).put(Const.of(qualifier.toString()), nonbindingMembersSet);
                    });

                    // context instances
                    LocalVar contextInstances;
                    if (scopeToContextInstances.isEmpty()) {
                        contextInstances = bc.localVar("contextInstances", bc.mapOf());
                    } else {
                        LocalVar contextInstancesFinal = bc.localVar("contextInstances", bc.new_(HashMap.class));
                        scopeToContextInstances.forEach((scopeClass, contextClass) -> {
                            Expr contextSupplier = bc.lambda(Supplier.class, lc -> {
                                lc.body(lbc -> {
                                    lbc.return_(lbc.new_(ConstructorDesc.of(ClassDesc.of(contextClass))));
                                });
                            });
                            bc.withMap(contextInstancesFinal).put(Const.of(classDescOf(scopeClass)), contextSupplier);
                        });
                        contextInstances = contextInstancesFinal;
                    }

                    bc.return_(bc.new_(ConstructorDesc.of(Components.class, Collection.class, Collection.class,
                            Collection.class, Set.class, Map.class, Supplier.class, Map.class, Set.class, Map.class),
                            beans, observers, contexts, interceptorBindings, transitiveBindings,
                            removedBeansSupplier, qualifiersNonbindingMembers, qualifiers, contextInstances));
                });
            });
        });
    }

    private void generateAddBeans(String generatedName, Gizmo gizmo, CodeGenInfo info,
            Map<BeanInfo, String> beanToGeneratedName) {
        Set<BeanInfo> processed = new HashSet<>();

        for (Container<BeanGroup> container : info.beans()) {
            gizmo.class_(container.className(generatedName, ADD_BEANS), cc -> {
                cc.final_();
                for (BeanGroup group : container) {
                    cc.staticMethod(ADD_BEANS + group.id(), mc -> {
                        mc.packagePrivate();
                        mc.returning(void.class);
                        ParamVar beanIdToBean = mc.parameter("beanIdToBean", Map.class);
                        mc.body(bc -> {
                            for (BeanInfo bean : group.beans()) {
                                ClassDesc beanType = beanToGeneratedName.containsKey(bean)
                                        ? ClassDesc.of(beanToGeneratedName.get(bean))
                                        : null;
                                if (beanType == null) {
                                    throw new IllegalStateException("No bean type found for: " + bean);
                                }

                                List<InjectionPointInfo> injectionPoints = bean.getInjections()
                                        .stream()
                                        .flatMap(i -> i.injectionPoints.stream())
                                        .filter(ip -> !ip.isDelegate() && !BuiltinBean.resolvesTo(ip))
                                        .toList();
                                List<ClassDesc> params = new ArrayList<>();
                                List<Expr> args = new ArrayList<>();

                                if (bean.isProducer()) {
                                    params.add(ClassDesc.of(Supplier.class.getName()));
                                    if (processed.contains(bean.getDeclaringBean())) {
                                        args.add(bc.withMap(beanIdToBean)
                                                .get(Const.of(bean.getDeclaringBean().getIdentifier())));
                                    } else {
                                        // Declaring bean was not processed yet - use MapValueSupplier
                                        args.add(bc.new_(MethodDescs.MAP_VALUE_SUPPLIER_CONSTRUCTOR, beanIdToBean,
                                                Const.of(bean.getDeclaringBean().getIdentifier())));
                                    }
                                }
                                for (InjectionPointInfo injectionPoint : injectionPoints) {
                                    params.add(ClassDesc.of(Supplier.class.getName()));
                                    if (processed.contains(injectionPoint.getResolvedBean())) {
                                        args.add(bc.withMap(beanIdToBean).get(
                                                Const.of(injectionPoint.getResolvedBean().getIdentifier())));
                                    } else {
                                        // Dependency was not processed yet - use MapValueSupplier
                                        args.add(bc.new_(MethodDescs.MAP_VALUE_SUPPLIER_CONSTRUCTOR, beanIdToBean,
                                                Const.of(injectionPoint.getResolvedBean().getIdentifier())));
                                    }
                                }
                                if (bean.getDisposer() != null) {
                                    for (InjectionPointInfo injectionPoint : bean.getDisposer()
                                            .getInjection().injectionPoints) {
                                        if (BuiltinBean.resolvesTo(injectionPoint)) {
                                            continue;
                                        }
                                        params.add(ClassDesc.of(Supplier.class.getName()));
                                        args.add(bc.new_(MethodDescs.MAP_VALUE_SUPPLIER_CONSTRUCTOR, beanIdToBean,
                                                Const.of(injectionPoint.getResolvedBean().getIdentifier())));
                                    }
                                }
                                for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                                    params.add(ClassDesc.of(Supplier.class.getName()));
                                    if (processed.contains(interceptor)) {
                                        args.add(bc.withMap(beanIdToBean).get(Const.of(interceptor.getIdentifier())));
                                    } else {
                                        // Bound interceptor was not processed yet - use MapValueSupplier
                                        args.add(bc.new_(MethodDescs.MAP_VALUE_SUPPLIER_CONSTRUCTOR, beanIdToBean,
                                                Const.of(interceptor.getIdentifier())));
                                    }
                                }
                                for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                                    params.add(ClassDesc.of(Supplier.class.getName()));
                                    if (processed.contains(decorator)) {
                                        args.add(bc.withMap(beanIdToBean).get(Const.of(decorator.getIdentifier())));
                                    } else {
                                        // Bound decorator was not processed yet - use MapValueSupplier
                                        args.add(bc.new_(MethodDescs.MAP_VALUE_SUPPLIER_CONSTRUCTOR, beanIdToBean,
                                                Const.of(decorator.getIdentifier())));
                                    }
                                }

                                // Foo_Bean bean = new Foo_Bean(bean3)
                                Expr beanInstance = bc.new_(ConstructorDesc.of(beanType, params), args);
                                // beans.put(id, bean)
                                bc.withMap(beanIdToBean).put(Const.of(bean.getIdentifier()), beanInstance);

                                processed.add(bean);
                            }

                            bc.return_();
                        });
                    });
                }
            });
        }
    }

    private void generateAddObservers(String generatedName, Gizmo gizmo, CodeGenInfo info,
            Map<ObserverInfo, String> observerToGeneratedName) {

        for (Container<ObserverGroup> container : info.observers()) {
            gizmo.class_(container.className(generatedName, ADD_OBSERVERS), cc -> {
                cc.final_();
                for (ObserverGroup group : container) {
                    cc.staticMethod(ADD_OBSERVERS + group.id(), mc -> {
                        mc.packagePrivate();
                        mc.returning(void.class);
                        ParamVar beanIdToBean = mc.parameter("beanIdToBean", Map.class);
                        ParamVar observers = mc.parameter("observers", List.class);
                        mc.body(bc -> {
                            for (ObserverInfo observer : group.observers()) {
                                ClassDesc observerType = observerToGeneratedName.containsKey(observer)
                                        ? ClassDesc.of(observerToGeneratedName.get(observer))
                                        : null;
                                if (observerType == null) {
                                    throw new IllegalStateException("No observer type found for: " + observerType);
                                }
                                List<ClassDesc> params = new ArrayList<>();
                                List<Expr> args = new ArrayList<>();

                                if (!observer.isSynthetic()) {
                                    List<InjectionPointInfo> injectionPoints = observer.getInjection().injectionPoints.stream()
                                            .filter(ip -> !BuiltinBean.resolvesTo(ip))
                                            .toList();
                                    // declaring bean
                                    params.add(ClassDesc.of(Supplier.class.getName()));
                                    args.add(bc.withMap(beanIdToBean)
                                            .get(Const.of(observer.getDeclaringBean().getIdentifier())));
                                    // injections
                                    for (InjectionPointInfo injectionPoint : injectionPoints) {
                                        params.add(ClassDesc.of(Supplier.class.getName()));
                                        args.add(bc.withMap(beanIdToBean).get(
                                                Const.of(injectionPoint.getResolvedBean().getIdentifier())));
                                    }
                                }
                                Expr observerInstance = bc.new_(ConstructorDesc.of(observerType, params), args);
                                bc.withList(observers).add(observerInstance);
                            }
                            bc.return_();
                        });
                    });
                }
            });
        }
    }

    private void generateAddRemovedBeans(String generatedName, Gizmo gizmo, CodeGenInfo info) {
        for (Container<RemovedBeanGroup> container : info.removedBeans()) {

            gizmo.class_(container.className(generatedName, ADD_REMOVED_BEANS), cc -> {
                cc.final_();

                for (RemovedBeanGroup group : container) {
                    cc.staticMethod(ADD_REMOVED_BEANS + group.id(), mc -> {
                        mc.public_(); // to allow access from an anonymous class
                        mc.returning(void.class);
                        ParamVar rtRemovedBeans = mc.parameter("removedBeans", List.class);
                        ParamVar typeCacheMap = mc.parameter("typeCache", Map.class);
                        mc.body(b0 -> {
                            LocalVar tccl = b0.localVar("tccl",
                                    b0.invokeVirtual(MethodDescs.THREAD_GET_TCCL, b0.currentThread()));

                            Map<AnnotationInstanceEquivalenceProxy, LocalVar> sharedQualifers = new HashMap<>();
                            for (BeanInfo btRemovedBean : group.removedBeans()) {
                                // Bean types
                                LocalVar rtTypes = b0.localVar("types", b0.new_(HashSet.class));
                                for (Type btType : btRemovedBean.getTypes()) {
                                    if (DotNames.OBJECT.equals(btType.name())) {
                                        // Skip java.lang.Object
                                        continue;
                                    }

                                    b0.try_(tc -> {
                                        tc.body(b1 -> {
                                            try {
                                                LocalVar rtType = RuntimeTypeCreator.of(b1)
                                                        .withCache(typeCacheMap)
                                                        .withTCCL(tccl)
                                                        .create(btType);
                                                b1.withSet(rtTypes).add(rtType);
                                            } catch (IllegalArgumentException e) {
                                                throw new IllegalStateException("Unable to construct type for " + btRemovedBean
                                                        + ": " + e.getMessage());
                                            }
                                        });
                                        tc.catch_(Throwable.class, "e", (b1, e) -> {
                                            b1.invokeStatic(MethodDescs.COMPONENTS_PROVIDER_UNABLE_TO_LOAD_REMOVED_BEAN_TYPE,
                                                    Const.of(btType.toString()), e);
                                        });
                                    });
                                }

                                // Qualifiers
                                LocalVar rtQualifiers;
                                if (btRemovedBean.hasDefaultQualifiers() || btRemovedBean.getQualifiers().isEmpty()) {
                                    // No qualifiers or default qualifiers (@Any, @Default)
                                    rtQualifiers = b0.localVar("qualifiers", Const.ofNull(Set.class));
                                } else {
                                    rtQualifiers = b0.localVar("qualifiers", b0.new_(HashSet.class));

                                    for (AnnotationInstance btQualifier : btRemovedBean.getQualifiers()) {
                                        if (DotNames.ANY.equals(btQualifier.name())) {
                                            // Skip @Any
                                            continue;
                                        }
                                        BuiltinQualifier btBuiltinQualifier = BuiltinQualifier.of(btQualifier);
                                        if (btBuiltinQualifier != null) {
                                            // Use the literal instance for built-in qualifiers
                                            b0.withSet(rtQualifiers).add(btBuiltinQualifier.getLiteralInstance());
                                        } else {
                                            LocalVar rtSharedQualifier = sharedQualifers
                                                    .get(btQualifier.createEquivalenceProxy());
                                            if (rtSharedQualifier == null) {
                                                // Create annotation literal first
                                                ClassInfo btQualifierClass = btRemovedBean.getDeployment().getQualifier(
                                                        btQualifier.name());
                                                LocalVar rtQualifier = b0.localVar("qualifier",
                                                        annotationLiterals.create(b0, btQualifierClass, btQualifier));
                                                b0.withSet(rtQualifiers).add(rtQualifier);
                                                sharedQualifers.put(btQualifier.createEquivalenceProxy(), rtQualifier);
                                            } else {
                                                b0.withSet(rtQualifiers).add(rtSharedQualifier);
                                            }
                                        }
                                    }
                                }

                                InjectableBean.Kind kind;
                                String description = null;
                                if (btRemovedBean.isClassBean()) {
                                    // This is the default
                                    kind = null;
                                } else if (btRemovedBean.isProducerField()) {
                                    kind = InjectableBean.Kind.PRODUCER_FIELD;
                                    description = btRemovedBean.getTarget().get().asField().declaringClass().name() + "#"
                                            + btRemovedBean.getTarget().get().asField().name();
                                } else if (btRemovedBean.isProducerMethod()) {
                                    kind = InjectableBean.Kind.PRODUCER_METHOD;
                                    description = btRemovedBean.getTarget().get().asMethod().declaringClass().name() + "#"
                                            + btRemovedBean.getTarget().get().asMethod().name() + "()";
                                } else {
                                    // unused interceptors/decorators are removed, but they are not treated
                                    // as unused beans and do not appear here
                                    kind = InjectableBean.Kind.SYNTHETIC;
                                }

                                Expr rtKind = kind != null
                                        ? Expr.staticField(FieldDesc.of(InjectableBean.Kind.class, kind.name()))
                                        : Const.ofNull(InjectableBean.Kind.class);
                                Expr rtRemovedBean = b0.new_(MethodDescs.REMOVED_BEAN_IMPL, rtKind,
                                        description != null ? Const.of(description) : Const.ofNull(String.class),
                                        rtTypes, rtQualifiers);
                                b0.withList(rtRemovedBeans).add(rtRemovedBean);
                            }
                            b0.return_();
                        });
                    });
                }
            });
        }
    }

    record BeanGroup(int id, List<BeanInfo> beans) {
    }

    record ObserverGroup(int id, List<ObserverInfo> observers) {
    }

    record RemovedBeanGroup(int id, List<BeanInfo> removedBeans) {
    }

    record Container<GROUP>(int id, List<GROUP> groups) implements Iterable<GROUP> {

        String className(String generatedName, String prefix) {
            return generatedName + "_" + prefix + id();
        }

        @Override
        public Iterator<GROUP> iterator() {
            return groups().iterator();
        }
    }

    record CodeGenInfo(List<Container<BeanGroup>> beans, List<Container<ObserverGroup>> observers,
            List<Container<RemovedBeanGroup>> removedBeans) {

    }

    private CodeGenInfo preprocess(BeanDeployment deployment) {
        List<BeanInfo> beans = preprocessBeans(deployment);
        List<BeanGroup> beanGroups = Grouping.of(beans, BEAN_GROUP_SIZE, BeanGroup::new);
        List<Container<BeanGroup>> beanContainers = Grouping.of(beanGroups, CONTAINER_SIZE, Container::new);
        List<ObserverInfo> observers = orderedObservers(deployment.getObservers());
        List<ObserverGroup> observerGroups = Grouping.of(observers, OBSERVER_GROUP_SIZE, ObserverGroup::new);
        List<Container<ObserverGroup>> observerContainers = Grouping.of(observerGroups, CONTAINER_SIZE, Container::new);
        List<BeanInfo> removedBeans = orderedBeans(deployment.getRemovedBeans());
        List<RemovedBeanGroup> removedBeanGroups = Grouping.of(removedBeans, REMOVED_BEAN_GROUP_SIZE, RemovedBeanGroup::new);
        List<Container<RemovedBeanGroup>> removedBeanContainers = Grouping.of(removedBeanGroups, CONTAINER_SIZE,
                Container::new);
        return new CodeGenInfo(beanContainers, observerContainers, removedBeanContainers);
    }

    private List<BeanInfo> preprocessBeans(BeanDeployment deployment) {
        Map<BeanInfo, List<BeanInfo>> dependencyMap = initBeanDependencyMap(deployment);

        // - iterate over dependencyMap entries and process beans for which all dependencies were already processed
        // - when a bean is processed the map entry is removed
        // - if we're stuck and the map is not empty, we found a circular dependency (and throw an ISE)
        Predicate<BeanInfo> isNotDependencyPredicate = new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo b) {
                return !isDependency(b, dependencyMap);
            }
        };
        Predicate<BeanInfo> isNormalScopedOrNotDependencyPredicate = new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo b) {
                return b.getScope().isNormal() || !isDependency(b, dependencyMap);
            }
        };
        Predicate<BeanInfo> isNotProducerOrNormalScopedOrNotDependencyPredicate = new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo b) {
                // Try to process non-producer beans first, including declaring beans of producers
                if (b.isProducer()) {
                    return false;
                }
                return b.getScope().isNormal() || !isDependency(b, dependencyMap);
            }
        };

        List<BeanInfo> result = new ArrayList<>();
        Set<BeanInfo> processed = new HashSet<>();

        boolean stuck = false;
        while (!dependencyMap.isEmpty()) {
            if (stuck) {
                throw circularDependenciesNotSupportedException(dependencyMap);
            }
            stuck = true;
            // First try to process beans that are not dependencies
            stuck = addBeans(result, dependencyMap, processed, isNotDependencyPredicate);
            if (stuck) {
                // It seems we're stuck but we can try to process normal scoped beans that can prevent a circular dependency
                stuck = addBeans(result, dependencyMap, processed, isNotProducerOrNormalScopedOrNotDependencyPredicate);
                if (stuck) {
                    stuck = addBeans(result, dependencyMap, processed, isNormalScopedOrNotDependencyPredicate);
                }
            }
        }

        // Finally process beans and interceptors that are not dependencies
        // We need to iterate in a deterministic order for build time reproducibility
        for (BeanInfo bean : orderedBeans(deployment.getBeans())) {
            if (!processed.contains(bean)) {
                result.add(bean);
            }
        }
        for (BeanInfo interceptor : orderedInterceptors(deployment.getInterceptors())) {
            if (!processed.contains(interceptor)) {
                result.add(interceptor);
            }
        }
        for (BeanInfo decorator : orderedDecorators(deployment.getDecorators())) {
            if (!processed.contains(decorator)) {
                result.add(decorator);
            }
        }

        return result;
    }

    /**
     * Returns a dependency map for bean instantiation. Say the following beans exist:
     *
     * <pre>
     * class Foo {
     *     &#064;Inject
     *     Bar bar;
     *
     *     &#064;Inject
     *     Baz baz;
     * }
     *
     * class Bar {
     *     &#064;Inject
     *     Baz baz;
     * }
     *
     * class Baz {
     * }
     * </pre>
     *
     * To create an instance of {@code Foo}, instances of {@code Bar} and {@code Baz} must already exist.
     * Further, to create an instance of {@code Bar}, an instance of {@code Baz} must already exist.
     * The returned map contains this information in the reverse form:
     *
     * <pre>
     * Foo -> []
     * Bar -> [Foo]
     * Baz -> [Foo, Bar]
     * </pre>
     *
     * The key in this map is a bean and the value is a list of beans that depend on the key. In other words,
     * the key is a dependency and the value is a list of its dependants.
     */
    private Map<BeanInfo, List<BeanInfo>> initBeanDependencyMap(BeanDeployment beanDeployment) {
        Function<BeanInfo, List<BeanInfo>> newArrayList = ignored -> new ArrayList<>();

        // We need to iterate in a deterministic order for build time reproducibility
        Map<BeanInfo, List<BeanInfo>> beanToInjections = new TreeMap<>(Reproducibility.BEAN_COMPARATOR);
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (bean.isProducer() && !bean.isStaticProducer()) {
                // `static` producer doesn't depend on its declaring bean
                beanToInjections.computeIfAbsent(bean.getDeclaringBean(), newArrayList).add(bean);
            }
            for (Injection injection : bean.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), newArrayList).add(bean);
                    }
                }
            }
            if (bean.getDisposer() != null) {
                for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), newArrayList).add(bean);
                    }
                }
            }
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                beanToInjections.computeIfAbsent(interceptor, newArrayList).add(bean);
            }
            for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                beanToInjections.computeIfAbsent(decorator, newArrayList).add(bean);
            }
        }
        // Also process interceptor and decorator injection points
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Injection injection : interceptor.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), newArrayList)
                                .add(interceptor);
                    }
                }
            }
        }
        for (DecoratorInfo decorator : beanDeployment.getDecorators()) {
            for (Injection injection : decorator.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!injectionPoint.isDelegate() && !BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), newArrayList)
                                .add(decorator);
                    }
                }
            }
        }
        // Note that we do not have to process observer injection points because observers are always processed after all beans are ready
        return beanToInjections;
    }

    private IllegalStateException circularDependenciesNotSupportedException(Map<BeanInfo, List<BeanInfo>> beanToInjections) {
        StringBuilder msg = new StringBuilder("Circular dependencies not supported: \n");
        for (Map.Entry<BeanInfo, List<BeanInfo>> e : beanToInjections.entrySet()) {
            msg.append("\t ");
            msg.append(e.getKey());
            msg.append(" injected into: ");
            msg.append(e.getValue()
                    .stream()
                    .map(BeanInfo::getBeanClass).map(Object::toString)
                    .collect(Collectors.joining(", ")));
            msg.append("\n");
        }
        return new IllegalStateException(msg.toString());
    }

    private boolean addBeans(List<BeanInfo> list, Map<BeanInfo, List<BeanInfo>> dependencyMap,
            Set<BeanInfo> processed, Predicate<BeanInfo> filter) {
        boolean stuck = true;
        for (Iterator<BeanInfo> iterator = dependencyMap.keySet().iterator(); iterator.hasNext();) {
            BeanInfo bean = iterator.next();
            if (filter.test(bean)) {
                iterator.remove();
                list.add(bean);
                processed.add(bean);
                stuck = false;
            }
        }
        return stuck;
    }

    private boolean isDependency(BeanInfo bean, Map<BeanInfo, List<BeanInfo>> dependencyMap) {
        for (List<BeanInfo> dependants : dependencyMap.values()) {
            if (dependants.contains(bean)) {
                return true;
            }
        }
        return false;
    }
}
