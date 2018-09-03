package org.jboss.protean.arc.processor;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.BeanProvider;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InjectableInterceptor;
import org.jboss.protean.arc.InjectableReferenceProvider;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.objectweb.asm.Type;

/**
 *
 * @author Martin Kouba
 */
public class BeanProviderGenerator extends AbstractGenerator {

    static final String BEAN_PROVIDER_SUFFIX = "_BeanProvider";

    static final String SETUP_PACKAGE = Arc.class.getPackage().getName() + ".setup";

    /**
     *
     * @param name
     * @param beanDeployment
     * @param beanToGeneratedName
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, Map<BeanInfo, String> beanToGeneratedName) {

        ResourceClassOutput classOutput = new ResourceClassOutput();

        String generatedName = SETUP_PACKAGE + "." + name + BEAN_PROVIDER_SUFFIX;
        ClassCreator beanProvider = ClassCreator.builder().classOutput(classOutput).className(generatedName).interfaces(BeanProvider.class).build();

        // BeanProvider#getBeans()
        MethodCreator getBeans = beanProvider.getMethodCreator("getBeans", Collection.class);
        // List<InjectableBean<?>> beans = new ArrayList<>();
        ResultHandle beansHandle = getBeans.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));

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
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                beanToInjections.computeIfAbsent(interceptor, d -> new ArrayList<>()).add(bean);
            }
        }
        // Also process interceptors injection points
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            for (Injection injection : interceptor.getInjections()) {
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    if (!BuiltinBean.resolvesTo(injectionPoint)) {
                        beanToInjections.computeIfAbsent(injectionPoint.getResolvedBean(), d -> new ArrayList<>()).add(interceptor);
                    }
                }
            }
        }

        Map<BeanInfo, ResultHandle> beanToResultHandle = new HashMap<>();
        AtomicInteger localNameIdx = new AtomicInteger();
        List<BeanInfo> processed = new ArrayList<>();

        // TODO handle circular dependencies
        while (!beanToInjections.isEmpty()) {
            for (Iterator<Entry<BeanInfo, List<BeanInfo>>> iterator = beanToInjections.entrySet().iterator(); iterator.hasNext();) {
                Entry<BeanInfo, List<BeanInfo>> entry = iterator.next();
                BeanInfo bean = entry.getKey();
                if (!isDependency(bean, beanToInjections)) {
                    addBean(getBeans, beansHandle, bean, beanToGeneratedName, localNameIdx, beanToResultHandle);
                    iterator.remove();
                    processed.add(bean);
                }
            }
        }
        // Finally process beans that are not dependencies
        for (BeanInfo bean : beanDeployment.getBeans()) {
            if (!processed.contains(bean)) {
                addBean(getBeans, beansHandle, bean, beanToGeneratedName, localNameIdx, beanToResultHandle);
            }
        }
        // return beans
        getBeans.returnValue(beansHandle);

        // Finally write the bytecode
        beanProvider.close();

        List<Resource> resources = new ArrayList<>();
        for (Resource resource : classOutput.getResources()) {
            resources.add(resource);
            // TODO proper name conversion
            resources
                    .add(ResourceImpl.serviceProvider(BeanProvider.class.getName(), (resource.getName().replace("/", ".")).getBytes(Charset.forName("UTF-8"))));
        }
        return resources;
    }

    private void addBean(MethodCreator getBeans, ResultHandle beansResultHandle, BeanInfo bean, Map<BeanInfo, String> beanToGeneratedName,
            AtomicInteger localNameIdx, Map<BeanInfo, ResultHandle> beanToResultHandle) {

        String beanType = beanToGeneratedName.get(bean);

        List<InjectionPointInfo> injectionPoints = bean.getInjections().isEmpty() ? Collections.emptyList()
                : bean.getInjections().stream().flatMap(i -> i.injectionPoints.stream()).filter(ip -> !BuiltinBean.resolvesTo(ip)).collect(Collectors.toList());
        List<ResultHandle> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        if (bean.isProducerMethod() || bean.isProducerField()) {
            params.add(beanToResultHandle.get(bean.getDeclaringBean()));
            paramTypes.add(Type.getDescriptor(InjectableBean.class));
        }
        for (InjectionPointInfo injetionPoint : injectionPoints) {
            ResultHandle resultHandle = beanToResultHandle.get(injetionPoint.getResolvedBean());
            params.add(resultHandle);
            paramTypes.add(Type.getDescriptor(InjectableReferenceProvider.class));
        }
        if (!bean.getInterceptedMethods().isEmpty()) {
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                ResultHandle resultHandle = beanToResultHandle.get(interceptor);
                params.add(resultHandle);
                paramTypes.add(Type.getDescriptor(InjectableInterceptor.class));
            }
        }
        // Foo_Bean bean2 = new Foo_Bean(bean2)
        ResultHandle beanInstance = getBeans.newInstance(MethodDescriptor.ofConstructor(beanType, paramTypes.toArray(new String[0])),
                params.toArray(new ResultHandle[0]));
        // beans.add(bean2)
        getBeans.invokeInterfaceMethod(MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class), beansResultHandle, beanInstance);
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
