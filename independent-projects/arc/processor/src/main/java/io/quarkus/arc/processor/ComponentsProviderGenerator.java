package io.quarkus.arc.processor;

import static java.util.stream.Collectors.toList;
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

        // Map<String, Supplier<InjectableBean<?>>> beanIdToBeanSupplier = new HashMap<>();
        ResultHandle beanIdToBeanSupplierHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(HashMap.class));

        // Bar -> Foo, Baz
        // Foo -> Baz
        // Interceptor -> Baz
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

        Map<BeanInfo, ResultHandle> beanToResultSupplierHandle = new HashMap<>();
        List<BeanInfo> processed = new ArrayList<>();

        // At this point we are going to fill the beanIdToBeanSupplier map
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
            // First try to proces beans that are not dependencies
            stuck = addBeans(beanToInjections, processed, beanToResultSupplierHandle, getComponents, beanIdToBeanSupplierHandle,
                    beanToGeneratedName, b -> !isDependency(b, beanToInjections));
            if (stuck) {
                // It seems we're stuck but we can try to process normal scoped beans that can prevent a circular dependency
                stuck = addBeans(beanToInjections, processed, beanToResultSupplierHandle, getComponents,
                        beanIdToBeanSupplierHandle, beanToGeneratedName,
                        b -> {
                            // Try to process non-producer beans first, including declaring beans of producers
                            if (b.isProducerField() || b.isProducerMethod()) {
                                return false;
                            }
                            return b.getScope().isNormal() || !isDependency(b, beanToInjections);
                        });
                if (stuck) {
                    stuck = addBeans(beanToInjections, processed, beanToResultSupplierHandle, getComponents,
                            beanIdToBeanSupplierHandle, beanToGeneratedName,
                            b -> !isDependency(b, beanToInjections) || b.getScope().isNormal());
                }
            }
        }
        // Finally process beans and interceptors that are not dependencies
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (!processed.contains(bean)) {
                addBean(getComponents, beanIdToBeanSupplierHandle, bean, beanToGeneratedName, beanToResultSupplierHandle);
            }
        }
        for (BeanInfo interceptor : beanDeployment.getInterceptors()) {
            if (!processed.contains(interceptor)) {
                addBean(getComponents, beanIdToBeanSupplierHandle, interceptor, beanToGeneratedName,
                        beanToResultSupplierHandle);
            }
        }

        // Observers
        ResultHandle observersHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (ObserverInfo observer : beanDeployment.getObservers()) {
            String observerType = observerToGeneratedName.get(observer);
            List<InjectionPointInfo> injectionPoints = observer.getInjection().injectionPoints.stream()
                    .filter(ip -> !BuiltinBean.resolvesTo(ip))
                    .collect(toList());
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            ResultHandle resultSupplierHandle = beanToResultSupplierHandle.get(observer.getDeclaringBean());
            params.add(resultSupplierHandle);
            paramTypes.add(Type.getDescriptor(Supplier.class));
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                resultSupplierHandle = beanToResultSupplierHandle.get(injectionPoint.getResolvedBean());
                params.add(resultSupplierHandle);
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
            ResultHandle observerInstance = getComponents.newInstance(
                    MethodDescriptor.ofConstructor(observerType, paramTypes.toArray(new String[0])),
                    params.toArray(new ResultHandle[0]));
            getComponents.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, observersHandle, observerInstance);
        }

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

        // final Collection beans = beanIdToBeanSupplier.values();
        ResultHandle beansHandle = getComponents.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "values", Collection.class),
                beanIdToBeanSupplierHandle);

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
            // TODO proper name conversion
            resources.add(ResourceImpl.serviceProvider(ComponentsProvider.class.getName(),
                    (resource.getName().replace('/', '.')).getBytes(Charset.forName("UTF-8"))));
        }
        return resources;
    }

    private boolean addBeans(Map<BeanInfo, List<BeanInfo>> beanToInjections, List<BeanInfo> processed,
            Map<BeanInfo, ResultHandle> beanToResultSupplierHandle, MethodCreator getComponents,
            ResultHandle beanIdToBeanSupplierHandle, Map<BeanInfo, String> beanToGeneratedName, Predicate<BeanInfo> filter) {
        boolean stuck = true;
        for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
            BeanInfo bean = entry.getKey();
            if (filter.test(bean)) {
                addBean(getComponents, beanIdToBeanSupplierHandle, bean, beanToGeneratedName,
                        beanToResultSupplierHandle);
                iterator.remove();
                processed.add(bean);
                stuck = false;
            }
        }
        return stuck;
    }

    private void addBean(MethodCreator getComponents, ResultHandle beanIdToBeanSupplierHandle, BeanInfo bean,
            Map<BeanInfo, String> beanToGeneratedName,
            Map<BeanInfo, ResultHandle> beanToResultSupplierHandle) {

        String beanType = beanToGeneratedName.get(bean);

        List<InjectionPointInfo> injectionPoints = bean.getInjections().stream().flatMap(i -> i.injectionPoints.stream())
                .filter(ip -> !BuiltinBean.resolvesTo(ip)).collect(toList());
        List<ResultHandle> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        if (bean.isProducerMethod() || bean.isProducerField()) {
            ResultHandle resultSupplierHandle = beanToResultSupplierHandle.get(bean.getDeclaringBean());
            if (resultSupplierHandle == null) {
                throw new IllegalStateException(
                        "A supplier for a declaring bean of a producer bean is not available - most probaly an unsupported circular dependency use case \n - declaring bean: "
                                + bean.getDeclaringBean() + "\n - producer bean: " + bean);
            }
            params.add(resultSupplierHandle);
            paramTypes.add(Type.getDescriptor(Supplier.class));
        }
        for (InjectionPointInfo injectionPoint : injectionPoints) {

            // new MapValueSupplier(beanIdToBeanSupplier, id);
            ResultHandle beanIdHandle = getComponents.load(injectionPoint.getResolvedBean().getIdentifier());
            ResultHandle beanSupplierHandle = getComponents.newInstance(MethodDescriptors.MAP_VALUE_SUPPLIER_CONSTRUCTOR,
                    beanIdToBeanSupplierHandle, beanIdHandle);

            params.add(beanSupplierHandle);
            paramTypes.add(Type.getDescriptor(Supplier.class));
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                ResultHandle resultSupplierHandle = beanToResultSupplierHandle.get(injectionPoint.getResolvedBean());
                params.add(resultSupplierHandle);
                paramTypes.add(Type.getDescriptor(Supplier.class));
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            ResultHandle resultSupplierHandle = beanToResultSupplierHandle.get(interceptor);
            params.add(resultSupplierHandle);
            paramTypes.add(Type.getDescriptor(Supplier.class));
        }

        // Foo_Bean bean2 = new Foo_Bean(bean2)
        ResultHandle beanInstance = getComponents.newInstance(
                MethodDescriptor.ofConstructor(beanType, paramTypes.toArray(new String[0])),
                params.toArray(new ResultHandle[0]));
        // beans.put(..., bean2)
        final ResultHandle beanInfoHandle = getComponents.load(bean.getIdentifier());
        getComponents.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, beanIdToBeanSupplierHandle, beanInfoHandle,
                beanInstance);

        // Create a Supplier that will return the bean instance at runtime.
        ResultHandle beanInstanceSupplier = getComponents.newInstance(MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR,
                beanInstance);
        beanToResultSupplierHandle.put(bean, beanInstanceSupplier);
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

}
