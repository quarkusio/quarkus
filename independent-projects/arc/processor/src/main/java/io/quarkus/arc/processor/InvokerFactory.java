package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public class InvokerFactory {
    private final BeanDeployment beanDeployment;
    private final InjectionPointModifier injectionPointTransformer;

    InvokerFactory(BeanDeployment beanDeployment, InjectionPointModifier injectionPointTransformer) {
        this.beanDeployment = beanDeployment;
        this.injectionPointTransformer = injectionPointTransformer;
    }

    public InvokerBuilder createInvoker(BeanInfo targetBean, MethodInfo targetMethod) {
        Objects.requireNonNull(targetBean);
        Objects.requireNonNull(targetMethod);

        if (!targetBean.isClassBean()) {
            throw new DeploymentException("Cannot build invoker for target bean: " + targetBean);
        }
        if (targetBean.isInterceptor() || targetBean.isDecorator()) {
            throw new DeploymentException("Cannot build invoker for target bean: " + targetBean);
        }
        if (targetMethod.isSynthetic()
                || targetMethod.isConstructor()
                || targetMethod.isStaticInitializer()
                || Modifier.isPrivate(targetMethod.flags())) {
            throw new DeploymentException("Cannot build invoker for target method: " + targetMethod);
        }
        if (DotNames.OBJECT.equals(targetMethod.declaringClass().name()) && !Methods.TO_STRING.equals(targetMethod.name())) {
            throw new DeploymentException("Cannot build invoker for target method: " + targetMethod);
        }
        // verify that the `targetMethod` belongs to the `targetBean`
        boolean isOwnMethod = false;
        IndexView index = beanDeployment.getBeanArchiveIndex();
        Deque<ClassInfo> worklist = new ArrayDeque<>();
        worklist.add(targetBean.getImplClazz());
        while (!worklist.isEmpty()) {
            ClassInfo clazz = worklist.poll();
            if (clazz.methods().contains(targetMethod)) {
                isOwnMethod = true;
                break;
            }
            DotName superClassName = clazz.superName();
            if (!DotNames.OBJECT.equals(superClassName)) {
                ClassInfo superClass = index.getClassByName(superClassName);
                worklist.add(superClass);
            }
            for (DotName superInterfaceName : clazz.interfaceNames()) {
                ClassInfo superInterface = index.getClassByName(superInterfaceName);
                worklist.add(superInterface);
            }
        }
        if (!isOwnMethod) {
            throw new DeploymentException("Method does not belong to target bean " + targetBean + ": " + targetMethod);
        }
        return new InvokerBuilder(targetBean, targetMethod, beanDeployment, injectionPointTransformer);
    }
}
