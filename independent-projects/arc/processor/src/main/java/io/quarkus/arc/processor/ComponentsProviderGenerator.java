package io.quarkus.arc.processor;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
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

        // Maps a bean to all injection points it is resolved to 
        // Bar -> Foo, Baz
        // Foo -> Baz
        // Interceptor -> Baz
        Map<BeanInfo, List<BeanInfo>> beanToInjections = initBeanToInjections(beanDeployment);

        // Break bean processing into multiple addBeans() methods
        // Map<String, InjectableBean<?>>
        ResultHandle beanIdToBeanHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        processBeans(componentsProvider, getComponents, beanIdToBeanHandle, beanToInjections, beanToGeneratedName,
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

        ResultHandle transitiveBindingsHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        for (Entry<DotName, Set<AnnotationInstance>> entry : beanDeployment.getTransitiveInterceptorBindings().entrySet()) {
            ResultHandle bindingsHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance binding : entry.getValue()) {
                // Create annotation literals first
                ClassInfo bindingClass = beanDeployment.getInterceptorBinding(binding.name());
                getComponents.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                        annotationLiterals.process(getComponents, classOutput, bindingClass, binding,
                                SETUP_PACKAGE));
            }
            getComponents.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, transitiveBindingsHandle,
                    getComponents.loadClass(entry.getKey().toString()), bindingsHandle);
        }

        ResultHandle beansHandle = getComponents.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "values", Collection.class),
                beanIdToBeanHandle);

        // Break removed beans processing into multiple addRemovedBeans() methods
        ResultHandle removedBeansHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        if (detectUnusedFalsePositives) {
            processRemovedBeans(componentsProvider, getComponents, removedBeansHandle, beanDeployment, classOutput);
        }

        ResultHandle componentsHandle = getComponents.newInstance(
                MethodDescriptor.ofConstructor(Components.class, Collection.class, Collection.class, Collection.class,
                        Map.class, Collection.class),
                beansHandle, observersHandle, contextsHandle, transitiveBindingsHandle, removedBeansHandle);
        getComponents.returnValue(componentsHandle);

        // Finally write the bytecode
        componentsProvider.close();

        List<Resource> resources = new ArrayList<>();
        for (Resource resource : classOutput.getResources()) {
            resources.add(resource);
            resources.add(ResourceImpl.serviceProvider(ComponentsProvider.class.getName(),
                    (resource.getName().replace('/', '.')).getBytes(StandardCharsets.UTF_8), null));
        }
        return resources;
    }

    private void processBeans(ClassCreator componentsProvider, MethodCreator getComponents, ResultHandle beanIdToBeanHandle,
            Map<BeanInfo, List<BeanInfo>> beanToInjections,
            Map<BeanInfo, String> beanToGeneratedName, BeanDeployment beanDeployment) {

        Set<BeanInfo> processed = new HashSet<>();
        BeanAdder beanAdder = new BeanAdder(componentsProvider, getComponents, processed, beanIdToBeanHandle,
                beanToGeneratedName);

        // - iterate over beanToInjections entries and process beans for which all dependencies are already present in the map
        // - when a bean is processed the map entry is removed
        // - if we're stuck and the map is not empty ISE is thrown
        boolean stuck = false;
        while (!beanToInjections.isEmpty()) {
            if (stuck) {
                throw new IllegalStateException("Circular dependencies not supported: \n" + beanToInjections.entrySet()
                        .stream()
                        .map(e -> "\t " + e.getKey() + " injected into: " + e.getValue()
                                .stream()
                                .map(b -> b.getBeanClass()
                                        .toString())
                                .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("\n")));
            }
            stuck = true;
            // First try to process beans that are not dependencies
            stuck = addBeans(beanAdder, beanToInjections, processed, beanIdToBeanHandle,
                    beanToGeneratedName, b -> !isDependency(b, beanToInjections));
            if (stuck) {
                // It seems we're stuck but we can try to process normal scoped beans that can prevent a circular dependency
                stuck = addBeans(beanAdder, beanToInjections, processed, beanIdToBeanHandle,
                        beanToGeneratedName,
                        b -> {
                            // Try to process non-producer beans first, including declaring beans of producers
                            if (b.isProducerField() || b.isProducerMethod()) {
                                return false;
                            }
                            return b.getScope().isNormal() || !isDependency(b, beanToInjections);
                        });
                if (stuck) {
                    stuck = addBeans(beanAdder, beanToInjections, processed,
                            beanIdToBeanHandle, beanToGeneratedName,
                            b -> !isDependency(b, beanToInjections) || b.getScope().isNormal());
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

        // Make sure the last addBeans() method is closed properly
        beanAdder.close();
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

    private void processRemovedBeans(ClassCreator componentsProvider, MethodCreator getComponents,
            ResultHandle removedBeansHandle, BeanDeployment beanDeployment, ClassOutput classOutput) {
        try (RemovedBeanAdder removedBeanAdder = new RemovedBeanAdder(componentsProvider, getComponents, removedBeansHandle,
                classOutput)) {
            for (BeanInfo remnovedBean : beanDeployment.getRemovedBeans()) {
                removedBeanAdder.addComponent(remnovedBean);
            }
        }
    }

    private Map<BeanInfo, List<BeanInfo>> initBeanToInjections(BeanDeployment beanDeployment) {
        Map<BeanInfo, List<BeanInfo>> beanToInjections = new HashMap<>();
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (bean.isProducerMethod() || bean.isProducerField()) {
                beanToInjections.computeIfAbsent(bean.getDeclaringBean(), d -> new ArrayList<>()).add(bean);
            }
            for (Injection injection : bean.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), d -> new ArrayList<>()).add(bean);
                    }
                }
            }
            if (bean.getDisposer() != null) {
                for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), d -> new ArrayList<>()).add(bean);
                    }
                }
            }
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                beanToInjections.computeIfAbsent(interceptor, d -> new ArrayList<>()).add(bean);
            }
        }
        // Also process interceptors injection points
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Injection injection : interceptor.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), d -> new ArrayList<>())
                                .add(interceptor);
                    }
                }
            }
        }
        // Note that we do not have to process observer injection points because observers are always processed after all beans are ready 
        return beanToInjections;
    }

    private boolean addBeans(BeanAdder beanAdder,
            Map<BeanInfo, List<BeanInfo>> beanToInjections, Set<BeanInfo> processed,
            ResultHandle beanIdToBeanHandle, Map<BeanInfo, String> beanToGeneratedName, Predicate<BeanInfo> filter) {
        boolean stuck = true;
        for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
            BeanInfo bean = entry.getKey();
            if (filter.test(bean)) {
                iterator.remove();
                beanAdder.addComponent(bean);
                processed.add(bean);
                stuck = false;
            }
        }
        return stuck;
    }

    private boolean isDependency(BeanInfo bean, Map<BeanInfo, List<BeanInfo>> beanToInjections) {
        for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator.hasNext();) {
            Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
            if (entry.getValue().contains(bean)) {
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
            getComponentsMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, Map.class, List.class),
                    getComponentsMethod.getThis(), beanIdToBeanHandle, observersHandle);
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
        private final ClassOutput classOutput;
        private ResultHandle tccl;

        public RemovedBeanAdder(ClassCreator componentsProvider, MethodCreator getComponentsMethod,
                ResultHandle removedBeansHandle, ClassOutput classOutput) {
            super(getComponentsMethod, componentsProvider);
            this.removedBeansHandle = removedBeansHandle;
            this.classOutput = classOutput;
        }

        @Override
        MethodCreator newAddMethod() {
            MethodCreator addMethod = componentsProvider.getMethodCreator(ADD_REMOVED_BEANS + group++, void.class, List.class)
                    .setModifiers(ACC_PRIVATE);
            // Get the TCCL - we will use it later
            ResultHandle currentThread = addMethod
                    .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
            tccl = addMethod.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);
            return addMethod;
        }

        @Override
        void invokeAddMethod() {
            getComponentsMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, List.class),
                    getComponentsMethod.getThis(), removedBeansHandle);
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
                ResultHandle typeHandle;
                try {
                    typeHandle = Types.getTypeHandle(addMethod, type, tccl);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Unable to construct the type handle for " + removedBean + ": " + e.getMessage());
                }
                addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, typesHandle, typeHandle);
            }

            // Qualifiers
            ResultHandle qualifiersHandle;
            if (!removedBean.getQualifiers().isEmpty() && !removedBean.hasDefaultQualifiers()) {

                qualifiersHandle = addMethod.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

                for (AnnotationInstance qualifierAnnotation : removedBean.getQualifiers()) {
                    if (DotNames.ANY.equals(qualifierAnnotation.name())) {
                        // Skip @Any
                        continue;
                    }
                    BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                    if (qualifier != null) {
                        addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                                qualifier.getLiteralInstance(addMethod));
                    } else {
                        // Create annotation literal first
                        ClassInfo qualifierClass = removedBean.getDeployment().getQualifier(qualifierAnnotation.name());
                        addMethod.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                                annotationLiterals.process(addMethod, classOutput,
                                        qualifierClass, qualifierAnnotation,
                                        Types.getPackageName(componentsProvider.getClassName())));
                    }
                }
                qualifiersHandle = addMethod
                        .invokeStaticMethod(MethodDescriptors.COLLECTIONS_UNMODIFIABLE_SET, qualifiersHandle);
            } else {
                qualifiersHandle = addMethod.loadNull();
            }

            InjectableBean.Kind kind;
            String description = null;
            if (removedBean.isClassBean()) {
                kind = null;
                description = removedBean.getTarget().get().toString();
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
            getComponentsMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                            addMethod.getMethodDescriptor().getName(), void.class, Map.class),
                    getComponentsMethod.getThis(), beanIdToBeanHandle);
        }

        @Override
        void addComponentInternal(BeanInfo bean) {
            ResultHandle beanIdToBeanHandle = addMethod.getMethodParam(0);
            String beanType = beanToGeneratedName.get(bean);
            if (beanType == null) {
                throw new IllegalStateException("No bean type found for: " + bean);
            }

            List<InjectionPointInfo> injectionPoints = bean.getInjections().stream().flatMap(i -> i.injectionPoints.stream())
                    .filter(ip -> !BuiltinBean.resolvesTo(ip)).collect(toList());
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            if (bean.isProducerMethod() || bean.isProducerField()) {
                if (!processedBeans.contains(bean.getDeclaringBean())) {
                    throw new IllegalStateException(
                            "Declaring bean of a producer bean is not available - most probably an unsupported circular dependency use case \n - declaring bean: "
                                    + bean.getDeclaringBean() + "\n - producer bean: " + bean);
                }
                params.add(addMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        beanIdToBeanHandle, addMethod.load(bean.getDeclaringBean().getIdentifier())));
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
        protected final MethodCreator getComponentsMethod;
        protected final ClassCreator componentsProvider;

        public ComponentAdder(MethodCreator getComponentsMethod, ClassCreator componentsProvider) {
            this.group = 1;
            this.getComponentsMethod = getComponentsMethod;
            this.componentsProvider = componentsProvider;
        }

        public void close() {
            if (addMethod != null) {
                addMethod.returnValue(null);
            }
        }

        void addComponent(T component) {

            if (addMethod == null || componentsAdded >= GROUP_LIMIT) {
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

    }

}
