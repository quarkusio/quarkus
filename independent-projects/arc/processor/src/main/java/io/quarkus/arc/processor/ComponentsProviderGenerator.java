package io.quarkus.arc.processor;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.objectweb.asm.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

/**
 *
 * @author Martin Kouba
 */
public class ComponentsProviderGenerator extends AbstractGenerator {

    static final String COMPONENTS_PROVIDER_SUFFIX = "_ComponentsProvider";
    static final String SETUP_PACKAGE = Arc.class.getPackage().getName() + ".setup";
    static final String ADD_OBSERVERS = "addObservers";
    static final String ADD_REMOVED_BEANS = "addRemovedBeans";
    static final String ADD_BEANS = "addBeans";

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
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, Map<BeanInfo, String> beanToGeneratedName,
            Map<ObserverInfo, String> observerToGeneratedName) {

        ResourceClassOutput classOutput = new ResourceClassOutput(true, generateSources);

        String generatedName = SETUP_PACKAGE + "." + name + COMPONENTS_PROVIDER_SUFFIX;
        ClassCreator componentsProvider = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(ComponentsProvider.class).build();

        MethodCreator getComponents = componentsProvider.getMethodCreator("getComponents", Components.class)
                .setModifiers(ACC_PUBLIC);

        Map<BeanInfo, List<BeanInfo>> dependencyMap = initBeanDependencyMap(beanDeployment);

        // Break bean processing into multiple addBeans() methods
        // Map<String, InjectableBean<?>>
        ResultHandle beanIdToBeanHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        processBeans(componentsProvider, getComponents, beanIdToBeanHandle, dependencyMap, beanToGeneratedName,
                beanDeployment);

        // Break observers processing into multiple addObservers() methods
        ResultHandle observersHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        processObservers(componentsProvider, getComponents, beanDeployment, beanIdToBeanHandle, observersHandle,
                observerToGeneratedName);

        // Custom contexts
        ResultHandle contextsHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (Entry<ScopeInfo, Function<MethodCreator, ResultHandle>> entry : beanDeployment.getCustomContexts().entrySet()) {
            ResultHandle contextHandle = entry.getValue().apply(getComponents);
            getComponents.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, contextsHandle, contextHandle);
        }

        // All interceptor bindings
        ResultHandle interceptorBindings = getComponents.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (ClassInfo binding : beanDeployment.getInterceptorBindings()) {
            getComponents.invokeInterfaceMethod(MethodDescriptors.SET_ADD, interceptorBindings,
                    getComponents.load(binding.name().toString()));
        }

        // Transitive interceptor bindings
        ResultHandle transitiveBindingsHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        for (Entry<DotName, Set<AnnotationInstance>> entry : beanDeployment.getTransitiveInterceptorBindings().entrySet()) {
            ResultHandle bindingsHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance binding : entry.getValue()) {
                // Create annotation literals first
                ClassInfo bindingClass = beanDeployment.getInterceptorBinding(binding.name());
                getComponents.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                        annotationLiterals.create(getComponents, bindingClass, binding));
            }
            getComponents.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, transitiveBindingsHandle,
                    getComponents.loadClass(entry.getKey().toString()), bindingsHandle);
        }

        ResultHandle beansHandle = getComponents.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "values", Collection.class),
                beanIdToBeanHandle);

        // Break removed beans processing into multiple addRemovedBeans() methods
        FunctionCreator removedBeansSupplier = getComponents.createFunction(Supplier.class);
        BytecodeCreator removedBeansSupplierBytecode = removedBeansSupplier.getBytecode();
        ResultHandle removedBeansList = removedBeansSupplierBytecode
                .newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        ResultHandle typeCacheHandle = removedBeansSupplierBytecode.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        if (detectUnusedFalsePositives) {
            // Generate static addRemovedBeans() methods
            processRemovedBeans(componentsProvider, removedBeansSupplierBytecode, removedBeansList, typeCacheHandle,
                    beanDeployment,
                    classOutput);
        }
        removedBeansSupplierBytecode.returnValue(removedBeansList);

        // All qualifiers
        ResultHandle qualifiers = getComponents.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (ClassInfo qualifier : beanDeployment.getQualifiers()) {
            getComponents.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiers,
                    getComponents.load(qualifier.name().toString()));
        }

        // Qualifier non-binding members
        ResultHandle qualifiersNonbindingMembers = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        for (Entry<DotName, Set<String>> entry : beanDeployment.getQualifierNonbindingMembers().entrySet()) {
            ResultHandle nonbindingMembers = getComponents.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (String member : entry.getValue()) {
                getComponents.invokeInterfaceMethod(MethodDescriptors.SET_ADD, nonbindingMembers,
                        getComponents.load(member));
            }
            getComponents.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, qualifiersNonbindingMembers,
                    getComponents.load(entry.getKey().toString()), nonbindingMembers);
        }

        ResultHandle componentsHandle = getComponents.newInstance(
                MethodDescriptor.ofConstructor(Components.class, Collection.class, Collection.class, Collection.class,
                        Set.class, Map.class, Supplier.class, Map.class, Set.class),
                beansHandle, observersHandle, contextsHandle, interceptorBindings, transitiveBindingsHandle,
                removedBeansSupplier.getInstance(), qualifiersNonbindingMembers, qualifiers);
        getComponents.returnValue(componentsHandle);

        // Finally write the bytecode
        componentsProvider.close();

        List<Resource> resources = new ArrayList<>();
        for (Resource resource : classOutput.getResources()) {
            resources.add(resource);
            if (resource.getName().endsWith(COMPONENTS_PROVIDER_SUFFIX)) {
                // We need to filter out nested classes and functions
                resources.add(ResourceImpl.serviceProvider(ComponentsProvider.class.getName(),
                        (resource.getName().replace('/', '.')).getBytes(StandardCharsets.UTF_8), null));
            }
        }
        return resources;
    }

    private void processBeans(ClassCreator componentsProvider, MethodCreator getComponents, ResultHandle beanIdToBeanHandle,
            Map<BeanInfo, List<BeanInfo>> dependencyMap,
            Map<BeanInfo, String> beanToGeneratedName, BeanDeployment beanDeployment) {

        Set<BeanInfo> processed = new HashSet<>();
        BeanAdder beanAdder = new BeanAdder(componentsProvider, getComponents, processed, beanIdToBeanHandle,
                beanToGeneratedName);

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

        boolean stuck = false;
        while (!dependencyMap.isEmpty()) {
            if (stuck) {
                throw circularDependenciesNotSupportedException(dependencyMap);
            }
            stuck = true;
            // First try to process beans that are not dependencies
            stuck = addBeans(beanAdder, dependencyMap, processed, isNotDependencyPredicate);
            if (stuck) {
                // It seems we're stuck but we can try to process normal scoped beans that can prevent a circular dependency
                stuck = addBeans(beanAdder, dependencyMap, processed, isNotProducerOrNormalScopedOrNotDependencyPredicate);
                if (stuck) {
                    stuck = addBeans(beanAdder, dependencyMap, processed, isNormalScopedOrNotDependencyPredicate);
                }
            }
        }
        // Finally process beans and interceptors that are not dependencies
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (!processed.contains(bean)) {
                beanAdder.addComponent(bean);
            }
        }
        for (BeanInfo interceptor : beanDeployment.getInterceptors()) {
            if (!processed.contains(interceptor)) {
                beanAdder.addComponent(interceptor);
            }
        }
        for (BeanInfo decorator : beanDeployment.getDecorators()) {
            if (!processed.contains(decorator)) {
                beanAdder.addComponent(decorator);
            }
        }

        // Make sure the last addBeans() method is closed properly
        beanAdder.close();
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

    private void processObservers(ClassCreator componentsProvider, MethodCreator getComponents, BeanDeployment beanDeployment,
            ResultHandle beanIdToBeanHandle, ResultHandle observersHandle, Map<ObserverInfo, String> observerToGeneratedName) {
        try (ObserverAdder observerAdder = new ObserverAdder(componentsProvider, getComponents, observerToGeneratedName,
                beanIdToBeanHandle, observersHandle)) {
            for (ObserverInfo observer : beanDeployment.getObservers()) {
                observerAdder.addComponent(observer);
            }
        }
    }

    private void processRemovedBeans(ClassCreator componentsProvider, BytecodeCreator targetMethod,
            ResultHandle removedBeansHandle, ResultHandle typeCacheHandle, BeanDeployment beanDeployment,
            ClassOutput classOutput) {
        try (RemovedBeanAdder removedBeanAdder = new RemovedBeanAdder(componentsProvider, targetMethod, removedBeansHandle,
                typeCacheHandle, classOutput)) {
            for (BeanInfo remnovedBean : beanDeployment.getRemovedBeans()) {
                removedBeanAdder.addComponent(remnovedBean);
            }
        }
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
        Function<BeanInfo, List<BeanInfo>> newArrayList = new Function<BeanInfo, List<BeanInfo>>() {
            @Override
            public List<BeanInfo> apply(BeanInfo b) {
                return new ArrayList<>();
            }
        };

        Map<BeanInfo, List<BeanInfo>> beanToInjections = new HashMap<>();
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

    private boolean addBeans(BeanAdder beanAdder, Map<BeanInfo, List<BeanInfo>> beanToInjections,
            Set<BeanInfo> processed, Predicate<BeanInfo> filter) {
        boolean stuck = true;
        for (Iterator<BeanInfo> iterator = beanToInjections.keySet().iterator(); iterator.hasNext();) {
            BeanInfo bean = iterator.next();
            if (filter.test(bean)) {
                iterator.remove();
                beanAdder.addComponent(bean);
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

    static class ObserverAdder extends ComponentAdder<ObserverInfo> {

        private final Map<ObserverInfo, String> observerToGeneratedName;
        private final ResultHandle beanIdToBeanHandle;
        private final ResultHandle observersHandle;

        ObserverAdder(ClassCreator componentsProvider, MethodCreator getComponentsMethod,
                Map<ObserverInfo, String> observerToGeneratedName, ResultHandle beanIdToBeanHandle,
                ResultHandle observersHandle) {
            super(getComponentsMethod, componentsProvider);
            this.observerToGeneratedName = observerToGeneratedName;
            this.beanIdToBeanHandle = beanIdToBeanHandle;
            this.observersHandle = observersHandle;
        }

        @Override
        MethodCreator newAddMethod() {
            return componentsProvider
                    .getMethodCreator(ADD_OBSERVERS + group++, void.class, Map.class, List.class)
                    .setModifiers(ACC_PRIVATE);
        }

        @Override
        void invokeAddMethod() {
            targetMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, Map.class, List.class),
                    targetMethod.getThis(), beanIdToBeanHandle, observersHandle);
        }

        @Override
        void addComponentInternal(ObserverInfo observer) {
            ResultHandle beanIdToBeanHandle = addMethod.getMethodParam(0);
            ResultHandle observersHandle = addMethod.getMethodParam(1);

            String observerType = observerToGeneratedName.get(observer);
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            if (!observer.isSynthetic()) {
                List<InjectionPointInfo> injectionPoints = observer.getInjection().injectionPoints.stream()
                        .filter(ip -> !BuiltinBean.resolvesTo(ip))
                        .collect(toList());
                // First param - declaring bean
                params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        beanIdToBeanHandle, addMethod.load(observer.getDeclaringBean().getIdentifier())));
                paramTypes.add(Type.getDescriptor(Supplier.class));
                for (InjectionPointInfo injectionPoint : injectionPoints) {
                    params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                    paramTypes.add(Type.getDescriptor(Supplier.class));
                }
            }
            ResultHandle observerInstance = addMethod.newInstance(
                    MethodDescriptor.ofConstructor(observerType, paramTypes.toArray(new String[0])),
                    params.toArray(new ResultHandle[0]));
            addMethod.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, observersHandle, observerInstance);
        }

    }

    class RemovedBeanAdder extends ComponentAdder<BeanInfo> {

        private final ResultHandle removedBeansHandle;
        private final ResultHandle typeCacheHandle;
        private ResultHandle tccl;
        // Shared annotation literals for an individual addRemovedBeansX() method
        private final Map<AnnotationInstanceKey, ResultHandle> sharedQualifers;

        private final MapTypeCache typeCache;

        public RemovedBeanAdder(ClassCreator componentsProvider, BytecodeCreator targetMethod,
                ResultHandle removedBeansHandle, ResultHandle typeCacheHandle, ClassOutput classOutput) {
            super(targetMethod, componentsProvider);
            this.removedBeansHandle = removedBeansHandle;
            this.typeCacheHandle = typeCacheHandle;
            this.sharedQualifers = new HashMap<>();
            this.typeCache = new MapTypeCache();
        }

        @Override
        protected int groupLimit() {
            return 5;
        }

        @Override
        MethodCreator newAddMethod() {
            // Clear the shared maps for each addRemovedBeansX() method
            sharedQualifers.clear();

            // static void addRemovedBeans1(List removedBeans, List typeCache)
            MethodCreator addMethod = componentsProvider
                    .getMethodCreator(ADD_REMOVED_BEANS + group++, void.class, List.class, Map.class)
                    .setModifiers(ACC_STATIC);
            // Get the TCCL - we will use it later
            ResultHandle currentThread = addMethod
                    .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
            tccl = addMethod.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);

            typeCache.initialize(addMethod);

            return addMethod;
        }

        @Override
        void invokeAddMethod() {
            // Static methods are invoked from within the generated supplier
            targetMethod.invokeStaticMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, List.class, Map.class),
                    removedBeansHandle, typeCacheHandle);
        }

        @Override
        void addComponentInternal(BeanInfo removedBean) {

            ResultHandle removedBeansHandle = addMethod.getMethodParam(0);

            // Bean types
            ResultHandle typesHandle = addMethod.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (org.jboss.jandex.Type type : removedBean.getTypes()) {
                if (DotNames.OBJECT.equals(type.name())) {
                    // Skip java.lang.Object
                    continue;
                }

                TryBlock tryBlock = addMethod.tryBlock();
                CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
                catchBlock.invokeStaticInterfaceMethod(
                        MethodDescriptors.COMPONENTS_PROVIDER_UNABLE_TO_LOAD_REMOVED_BEAN_TYPE,
                        catchBlock.load(type.toString()), catchBlock.getCaughtException());
                AssignableResultHandle typeHandle = tryBlock.createVariable(Object.class);
                try {
                    Types.getTypeHandle(typeHandle, tryBlock, type, tccl, typeCache);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Unable to construct the type handle for " + removedBean + ": " + e.getMessage());
                }
                tryBlock.invokeInterfaceMethod(MethodDescriptors.SET_ADD, typesHandle, typeHandle);
            }

            // Qualifiers
            ResultHandle qualifiersHandle;
            if (removedBean.hasDefaultQualifiers() || removedBean.getQualifiers().isEmpty()) {
                // No or default qualifiers (@Any, @Default)
                qualifiersHandle = addMethod.loadNull();
            } else {
                qualifiersHandle = addMethod.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

                for (AnnotationInstance qualifierAnnotation : removedBean.getQualifiers()) {
                    if (DotNames.ANY.equals(qualifierAnnotation.name())) {
                        // Skip @Any
                        continue;
                    }
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        // Use the literal instance for built-in qualifiers
                        addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                                qualifier.getLiteralInstance(addMethod));
                    } else {
                        ResultHandle sharedQualifier = sharedQualifers.get(new AnnotationInstanceKey(qualifierAnnotation));
                        if (sharedQualifier == null) {
                            // Create annotation literal first
                            ClassInfo qualifierClass = removedBean.getDeployment().getQualifier(qualifierAnnotation.name());
                            ResultHandle qualifierHandle = annotationLiterals.create(addMethod, qualifierClass,
                                    qualifierAnnotation);
                            addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                                    qualifierHandle);
                            sharedQualifers.put(new AnnotationInstanceKey(qualifierAnnotation), qualifierHandle);
                        } else {
                            addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                                    sharedQualifier);
                        }
                    }
                }
            }

            InjectableBean.Kind kind;
            String description = null;
            if (removedBean.isClassBean()) {
                // This is the default
                kind = null;
            } else if (removedBean.isProducerField()) {
                kind = InjectableBean.Kind.PRODUCER_FIELD;
                description = removedBean.getTarget().get().asField().declaringClass().name() + "#"
                        + removedBean.getTarget().get().asField().name();
            } else if (removedBean.isProducerMethod()) {
                kind = InjectableBean.Kind.PRODUCER_METHOD;
                description = removedBean.getTarget().get().asMethod().declaringClass().name() + "#"
                        + removedBean.getTarget().get().asMethod().name() + "()";
            } else {
                // Interceptors are never removed
                kind = InjectableBean.Kind.SYNTHETIC;
            }

            ResultHandle kindHandle = kind != null ? addMethod.readStaticField(
                    FieldDescriptor.of(InjectableBean.Kind.class, kind.toString(), InjectableBean.Kind.class))
                    : addMethod.loadNull();
            ResultHandle removedBeanHandle = addMethod.newInstance(MethodDescriptors.REMOVED_BEAN_IMPL,
                    kindHandle, description != null ? addMethod.load(description) : addMethod.loadNull(),
                    typesHandle,
                    qualifiersHandle);
            addMethod.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, removedBeansHandle, removedBeanHandle);
        }

    }

    static class MapTypeCache implements Types.TypeCache {

        private ResultHandle mapHandle;

        @Override
        public void initialize(MethodCreator method) {
            this.mapHandle = method.getMethodParam(1);
        }

        @Override
        public ResultHandle get(org.jboss.jandex.Type type, BytecodeCreator bytecode) {
            return bytecode.invokeInterfaceMethod(MethodDescriptors.MAP_GET, mapHandle, bytecode.load(type.toString()));
        }

        @Override
        public void put(org.jboss.jandex.Type type, ResultHandle value, BytecodeCreator bytecode) {
            bytecode.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, mapHandle, bytecode.load(type.toString()), value);
        }

    }

    static class BeanAdder extends ComponentAdder<BeanInfo> {

        private final Set<BeanInfo> processedBeans;
        private final ResultHandle beanIdToBeanHandle;
        private final Map<BeanInfo, String> beanToGeneratedName;

        public BeanAdder(ClassCreator componentsProvider, MethodCreator getComponentsMethod, Set<BeanInfo> processed,
                ResultHandle beanIdToBeanHandle, Map<BeanInfo, String> beanToGeneratedName) {
            super(getComponentsMethod, componentsProvider);
            this.processedBeans = processed;
            this.beanIdToBeanHandle = beanIdToBeanHandle;
            this.beanToGeneratedName = beanToGeneratedName;
        }

        @Override
        MethodCreator newAddMethod() {
            return componentsProvider.getMethodCreator(ADD_BEANS + group++, void.class, Map.class)
                    .setModifiers(ACC_PRIVATE);
        }

        @Override
        void invokeAddMethod() {
            targetMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, Map.class),
                    targetMethod.getThis(), beanIdToBeanHandle);
        }

        @Override
        void addComponentInternal(BeanInfo bean) {
            ResultHandle beanIdToBeanHandle = addMethod.getMethodParam(0);
            String beanType = beanToGeneratedName.get(bean);
            if (beanType == null) {
                throw new IllegalStateException("No bean type found for: " + bean);
            }

            List<InjectionPointInfo> injectionPoints = bean.getInjections().stream().flatMap(i -> i.injectionPoints.stream())
                    .filter(ip -> !ip.isDelegate() && !BuiltinBean.resolvesTo(ip)).collect(toList());
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            if (bean.isProducer()) {
                if (processedBeans.contains(bean.getDeclaringBean())) {
                    params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addMethod.load(bean.getDeclaringBean().getIdentifier())));
                } else {
                    // Declaring bean was not processed yet - use MapValueSupplier
                    params.add(addMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addMethod.load(bean.getDeclaringBean().getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                if (processedBeans.contains(injectionPoint.getResolvedBean())) {
                    params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                } else {
                    // Dependency was not processed yet - use MapValueSupplier
                    params.add(addMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            if (bean.getDisposer() != null) {
                for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                    if (BuiltinBean.resolvesTo(injectionPoint)) {
                        continue;
                    }
                    params.add(addMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                    paramTypes.add(Type.getDescriptor(Supplier.class));
                }
            }
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                if (processedBeans.contains(interceptor)) {
                    params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addMethod.load(interceptor.getIdentifier())));
                } else {
                    // Bound interceptor was not processed yet - use MapValueSupplier
                    params.add(addMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addMethod.load(interceptor.getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                if (processedBeans.contains(decorator)) {
                    params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addMethod.load(decorator.getIdentifier())));
                } else {
                    // Bound decorator was not processed yet - use MapValueSupplier
                    params.add(addMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addMethod.load(decorator.getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }

            // Foo_Bean bean = new Foo_Bean(bean3)
            ResultHandle beanInstance = addMethod.newInstance(
                    MethodDescriptor.ofConstructor(beanType, paramTypes.toArray(new String[0])),
                    params.toArray(new ResultHandle[0]));
            // beans.put(id, bean)
            final ResultHandle beanIdHandle = addMethod.load(bean.getIdentifier());
            addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, beanIdToBeanHandle, beanIdHandle,
                    beanInstance);
        }

    }

    static abstract class ComponentAdder<T extends InjectionTargetInfo> implements AutoCloseable {

        private static final int GROUP_LIMIT = 30;
        protected int group;
        private int componentsAdded;
        protected MethodCreator addMethod;
        protected final BytecodeCreator targetMethod;
        protected final ClassCreator componentsProvider;

        public ComponentAdder(BytecodeCreator getComponentsMethod, ClassCreator componentsProvider) {
            this.group = 1;
            this.targetMethod = getComponentsMethod;
            this.componentsProvider = componentsProvider;
        }

        public void close() {
            if (addMethod != null) {
                addMethod.returnValue(null);
            }
        }

        void addComponent(T component) {

            if (addMethod == null || componentsAdded >= groupLimit()) {
                if (addMethod != null) {
                    addMethod.returnValue(null);
                }
                componentsAdded = 0;
                // First add next addX() method
                addMethod = newAddMethod();
                // Invoke addX() inside the getComponents() method
                invokeAddMethod();
            }
            componentsAdded++;

            // Append component to the addX() method body
            addComponentInternal(component);
        }

        abstract MethodCreator newAddMethod();

        abstract void invokeAddMethod();

        abstract void addComponentInternal(T component);

        protected int groupLimit() {
            return GROUP_LIMIT;
        }

    }

    // This wrapper is needed because AnnotationInstance#equals() compares the annotation target
    private static final class AnnotationInstanceKey {

        private final AnnotationInstance annotationInstance;

        AnnotationInstanceKey(AnnotationInstance annotationInstance) {
            this.annotationInstance = annotationInstance;
        }

        @Override
        public int hashCode() {
            return annotationInstance.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AnnotationInstanceKey other = (AnnotationInstanceKey) obj;
            return annotationInstance.name().equals(other.annotationInstance.name())
                    && annotationInstance.values().equals(other.annotationInstance.values());
        }

    }

}
