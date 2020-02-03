package io.quarkus.arc.processor;

import static java.util.stream.Collectors.toList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.nio.charset.Charset;
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
    static final String ADD_BEANS = "addBeans";

    protected final AnnotationLiteralProcessor annotationLiterals;

    public ComponentsProviderGenerator(AnnotationLiteralProcessor annotationLiterals) {
        this.annotationLiterals = annotationLiterals;
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

        ResourceClassOutput classOutput = new ResourceClassOutput(true);

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

        // Break observers processing into multiple ddObservers() methods
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

        ResultHandle componentsHandle = getComponents.newInstance(
                MethodDescriptor.ofConstructor(Components.class, Collection.class, Collection.class, Collection.class,
                        Map.class),
                beansHandle, observersHandle, contextsHandle, transitiveBindingsHandle);
        getComponents.returnValue(componentsHandle);

        // Finally write the bytecode
        componentsProvider.close();

        List<Resource> resources = new ArrayList<>();
        for (Resource resource : classOutput.getResources()) {
            resources.add(resource);
            resources.add(ResourceImpl.serviceProvider(ComponentsProvider.class.getName(),
                    (resource.getName().replace('/', '.')).getBytes(Charset.forName("UTF-8"))));
        }
        return resources;
    }

    private void processBeans(ClassCreator componentsProvider, MethodCreator getComponents, ResultHandle beanIdToBeanHandle,
            Map<BeanInfo, List<BeanInfo>> beanToInjections,
            Map<BeanInfo, String> beanToGeneratedName, BeanDeployment beanDeployment) {

        Set<BeanInfo> processed = new HashSet<>();
        BeanAdder beanAdder = new BeanAdder(componentsProvider, getComponents, processed);

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
                beanAdder.addBean(bean, beanIdToBeanHandle, beanToGeneratedName);
            }
        }
        for (BeanInfo interceptor : beanDeployment.getInterceptors()) {
            if (!processed.contains(interceptor)) {
                beanAdder.addBean(interceptor, beanIdToBeanHandle, beanToGeneratedName);
            }
        }

        // Make sure the last addBeans() method is closed properly
        beanAdder.close();
    }

    private void processObservers(ClassCreator componentsProvider, MethodCreator getComponents, BeanDeployment beanDeployment,
            ResultHandle beanIdToBeanHandle, ResultHandle observersHandle, Map<ObserverInfo, String> observerToGeneratedName) {
        try (ObserverAdder observerAdder = new ObserverAdder(componentsProvider, getComponents)) {
            for (ObserverInfo observer : beanDeployment.getObservers()) {
                observerAdder.addObserver(observer, beanIdToBeanHandle, observersHandle, observerToGeneratedName);
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
                beanAdder.addBean(bean, beanIdToBeanHandle, beanToGeneratedName);
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

    static class ObserverAdder implements AutoCloseable {

        private static final int GROUP_LIMIT = 30;
        private int group;
        private int observersAdded;
        private MethodCreator addObserversMethod;
        private final MethodCreator getComponentsMethod;
        private final ClassCreator componentsProvider;

        public ObserverAdder(ClassCreator componentsProvider, MethodCreator getComponentsMethod) {
            this.group = 1;
            this.getComponentsMethod = getComponentsMethod;
            this.componentsProvider = componentsProvider;
        }

        public void close() {
            if (addObserversMethod != null) {
                addObserversMethod.returnValue(null);
            }
        }

        void addObserver(ObserverInfo observer, ResultHandle beanIdToBeanHandle, ResultHandle observersHandle,
                Map<ObserverInfo, String> observerToGeneratedName) {

            if (addObserversMethod == null || observersAdded >= GROUP_LIMIT) {
                if (addObserversMethod != null) {
                    addObserversMethod.returnValue(null);
                }
                observersAdded = 0;
                // First add next addObservers(map) method
                addObserversMethod = componentsProvider
                        .getMethodCreator(ADD_OBSERVERS + group++, void.class, Map.class, List.class)
                        .setModifiers(ACC_PRIVATE);
                // Invoke addObservers(map) inside the getComponents() method
                getComponentsMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                                addObserversMethod.getMethodDescriptor().getName(), void.class, Map.class, List.class),
                        getComponentsMethod.getThis(), beanIdToBeanHandle, observersHandle);
            }
            observersAdded++;

            // Append to the addObservers() method body
            beanIdToBeanHandle = addObserversMethod.getMethodParam(0);
            observersHandle = addObserversMethod.getMethodParam(1);

            String observerType = observerToGeneratedName.get(observer);
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            if (!observer.isSynthetic()) {
                List<InjectionPointInfo> injectionPoints = observer.getInjection().injectionPoints.stream()
                        .filter(ip -> !BuiltinBean.resolvesTo(ip))
                        .collect(toList());
                // First param - declaring bean
                params.add(addObserversMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        beanIdToBeanHandle, addObserversMethod.load(observer.getDeclaringBean().getIdentifier())));
                paramTypes.add(Type.getDescriptor(Supplier.class));
                for (InjectionPointInfo injectionPoint : injectionPoints) {
                    params.add(addObserversMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addObserversMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                    paramTypes.add(Type.getDescriptor(Supplier.class));
                }
            }
            ResultHandle observerInstance = addObserversMethod.newInstance(
                    MethodDescriptor.ofConstructor(observerType, paramTypes.toArray(new String[0])),
                    params.toArray(new ResultHandle[0]));
            addObserversMethod.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, observersHandle, observerInstance);
        }

    }

    static class BeanAdder implements AutoCloseable {

        private static final int GROUP_LIMIT = 30;
        private int group;
        private int beansWritten;
        private MethodCreator addBeansMethod;
        private final MethodCreator getComponentsMethod;
        private final ClassCreator componentsProvider;
        private final Set<BeanInfo> processedBeans;

        public BeanAdder(ClassCreator componentsProvider, MethodCreator getComponentsMethod, Set<BeanInfo> processed) {
            this.group = 1;
            this.getComponentsMethod = getComponentsMethod;
            this.componentsProvider = componentsProvider;
            this.processedBeans = processed;
        }

        public void close() {
            if (addBeansMethod != null) {
                addBeansMethod.returnValue(null);
            }
        }

        void addBean(BeanInfo bean, ResultHandle beanIdToBeanHandle,
                Map<BeanInfo, String> beanToGeneratedName) {

            if (addBeansMethod == null || beansWritten >= GROUP_LIMIT) {
                if (addBeansMethod != null) {
                    addBeansMethod.returnValue(null);
                }
                beansWritten = 0;
                // First add next addBeans(map) method
                addBeansMethod = componentsProvider.getMethodCreator(ADD_BEANS + group++, void.class, Map.class)
                        .setModifiers(ACC_PRIVATE);
                // Invoke addBeans(map) inside the getComponents() method
                getComponentsMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(componentsProvider.getClassName(),
                                addBeansMethod.getMethodDescriptor().getName(), void.class, Map.class),
                        getComponentsMethod.getThis(), beanIdToBeanHandle);
            }
            beansWritten++;

            // Append to the addBeans() method body
            beanIdToBeanHandle = addBeansMethod.getMethodParam(0);
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
                params.add(addBeansMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                        beanIdToBeanHandle, addBeansMethod.load(bean.getDeclaringBean().getIdentifier())));
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                if (processedBeans.contains(injectionPoint.getResolvedBean())) {
                    params.add(addBeansMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addBeansMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                } else {
                    // Dependency was not processed yet - use MapValueSupplier
                    params.add(addBeansMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addBeansMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            if (bean.getDisposer() != null) {
                for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                    params.add(addBeansMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addBeansMethod.load(injectionPoint.getResolvedBean().getIdentifier())));
                    paramTypes.add(Type.getDescriptor(Supplier.class));
                }
            }
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                if (processedBeans.contains(interceptor)) {
                    params.add(addBeansMethod.invokeInterfaceMethod(MethodDescriptors.MAP_GET,
                            beanIdToBeanHandle, addBeansMethod.load(interceptor.getIdentifier())));
                } else {
                    // Bound interceptor was not processed yet - use MapValueSupplier
                    params.add(addBeansMethod.newInstance(
                            MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                            beanIdToBeanHandle, addBeansMethod.load(interceptor.getIdentifier())));
                }
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }

            // Foo_Bean bean = new Foo_Bean(bean3)
            ResultHandle beanInstance = addBeansMethod.newInstance(
                    MethodDescriptor.ofConstructor(beanType, paramTypes.toArray(new String[0])),
                    params.toArray(new ResultHandle[0]));
            // beans.put(id, bean)
            final ResultHandle beanIdHandle = addBeansMethod.load(bean.getIdentifier());
            addBeansMethod.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, beanIdToBeanHandle, beanIdHandle,
                    beanInstance);
        }

    }

}
