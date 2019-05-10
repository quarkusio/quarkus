/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;

/**
 *
 * @author Martin Kouba
 */
public class ComponentsProviderGenerator extends AbstractGenerator {

    static final String COMPONENTS_PROVIDER_SUFFIX = "_ComponentsProvider";

    static final String SETUP_PACKAGE = Arc.class.getPackage().getName() + ".setup";

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

        // List<InjectableBean<?>> beans = new ArrayList<>();
        ResultHandle beansHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));

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

        Map<BeanInfo, ResultHandle> beanToResultHandle = new HashMap<>();
        List<BeanInfo> processed = new ArrayList<>();

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
            for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator
                    .hasNext();) {
                Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
                BeanInfo bean = entry.getKey();
                if (!isDependency(bean, beanToInjections)) {
                    addBean(getComponents, beansHandle, bean, beanToGeneratedName, beanToResultHandle);
                    iterator.remove();
                    processed.add(bean);
                    stuck = false;
                }
            }
        }
        // Finally process beans and interceptors that are not dependencies
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (!processed.contains(bean)) {
                addBean(getComponents, beansHandle, bean, beanToGeneratedName, beanToResultHandle);
            }
        }
        for (BeanInfo interceptor : beanDeployment.getInterceptors()) {
            if (!processed.contains(interceptor)) {
                addBean(getComponents, beansHandle, interceptor, beanToGeneratedName, beanToResultHandle);
            }
        }

        // Observers
        ResultHandle observersHandle = getComponents.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (ObserverInfo observer : beanDeployment.getObservers()) {
            String observerType = observerToGeneratedName.get(observer);
            List<InjectionPointInfo> injectionPoints = observer.getInjection().injectionPoints.stream()
                    .filter(ip -> !BuiltinBean.resolvesTo(ip))
                    .collect(Collectors.toList());
            List<ResultHandle> params = new ArrayList<>();
            List<String> paramTypes = new ArrayList<>();

            params.add(beanToResultHandle.get(observer.getDeclaringBean()));
            paramTypes.add(Type.getDescriptor(InjectableBean.class));
            for (InjectionPointInfo injetionPoint : injectionPoints) {
                ResultHandle resultHandle = beanToResultHandle.get(injetionPoint.getResolvedBean());
                params.add(resultHandle);
                paramTypes.add(Type.getDescriptor(InjectableReferenceProvider.class));
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

        ResultHandle componentsHandle = getComponents.newInstance(
                MethodDescriptor.ofConstructor(Components.class, Collection.class, Collection.class, Collection.class),
                beansHandle, observersHandle, contextsHandle);
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

    private void addBean(MethodCreator getComponents, ResultHandle beansResultHandle, BeanInfo bean,
            Map<BeanInfo, String> beanToGeneratedName,
            Map<BeanInfo, ResultHandle> beanToResultHandle) {

        String beanType = beanToGeneratedName.get(bean);

        List<InjectionPointInfo> injectionPoints = bean.getInjections().isEmpty() ? Collections.emptyList()
                : bean.getInjections().stream().flatMap(i -> i.injectionPoints.stream())
                        .filter(ip -> !BuiltinBean.resolvesTo(ip)).collect(Collectors.toList());
        List<ResultHandle> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        if (bean.isProducerMethod() || bean.isProducerField()) {
            params.add(beanToResultHandle.get(bean.getDeclaringBean()));
            paramTypes.add(Type.getDescriptor(InjectableBean.class));
        }
        for (InjectionPointInfo injectionPoint : injectionPoints) {
            ResultHandle resultHandle = beanToResultHandle.get(injectionPoint.getResolvedBean());
            params.add(resultHandle);
            paramTypes.add(Type.getDescriptor(InjectableReferenceProvider.class));
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                ResultHandle resultHandle = beanToResultHandle.get(injectionPoint.getResolvedBean());
                params.add(resultHandle);
                paramTypes.add(Type.getDescriptor(InjectableReferenceProvider.class));
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            ResultHandle resultHandle = beanToResultHandle.get(interceptor);
            params.add(resultHandle);
            paramTypes.add(Type.getDescriptor(InjectableInterceptor.class));
        }
        // Foo_Bean bean2 = new Foo_Bean(bean2)
        ResultHandle beanInstance = getComponents.newInstance(
                MethodDescriptor.ofConstructor(beanType, paramTypes.toArray(new String[0])),
                params.toArray(new ResultHandle[0]));
        // beans.add(bean2)
        getComponents.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, beansResultHandle, beanInstance);
        beanToResultHandle.put(bean, beanInstance);
    }

    private boolean isDependency(BeanInfo bean, Map<BeanInfo, List<BeanInfo>> beanToInjections) {
        for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator.hasNext();) {
            Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
            if (entry.getKey().equals(bean)) {
                continue;
            } else if (entry.getValue().contains(bean)) {
                return true;
            }
        }
        return false;
    }

}
