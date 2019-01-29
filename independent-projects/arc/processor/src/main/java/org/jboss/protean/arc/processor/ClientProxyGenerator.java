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

package org.jboss.protean.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.ClientProxy;
import org.jboss.protean.arc.CreationalContextImpl;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InjectableContext;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.DescriptorUtils;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class ClientProxyGenerator extends AbstractGenerator {

    static final String CLIENT_PROXY_SUFFIX = "_ClientProxy";

    private final Predicate<DotName> applicationClassPredicate;

    public ClientProxyGenerator(Predicate<DotName> applicationClassPredicate) {
        this.applicationClassPredicate = applicationClassPredicate;
    }

    /**
     *
     * @param bean
     * @param beanClassName Fully qualified class name
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean, String beanClassName, ReflectionRegistration reflectionRegistration) {

        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()));

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String baseName = getBaseName(bean, beanClassName);
        String targetPackage = getPackageName(bean);
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + CLIENT_PROXY_SUFFIX;

        // Foo_ClientProxy extends Foo implements ClientProxy
        List<String> interfaces = new ArrayList<>();
        String superClass = Object.class.getName();
        interfaces.add(ClientProxy.class.getName());
        boolean isInterface = false;

        if (Modifier.isInterface(providerClass.flags())) {
            isInterface = true;
            interfaces.add(providerTypeName);
        } else {
            superClass = providerTypeName;
        }

        ClassCreator clientProxy = ClassCreator.builder().classOutput(classOutput).className(generatedName).superClass(superClass)
                .interfaces(interfaces.toArray(new String[0])).build();
        FieldCreator beanField = clientProxy.getFieldCreator("bean", DescriptorUtils.extToInt(beanClassName)).setModifiers(ACC_PRIVATE | ACC_FINAL);

        createConstructor(clientProxy, beanClassName, superClass, beanField.getFieldDescriptor());
        implementDelegate(clientProxy, providerTypeName, beanField.getFieldDescriptor());
        implementGetContextualInstance(clientProxy, providerTypeName);

        for (MethodInfo method : getDelegatingMethods(bean)) {

            MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
            MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName, 
                                                                       originalMethodDescriptor.getName(), 
                                                                       originalMethodDescriptor.getReturnType(), 
                                                                       originalMethodDescriptor.getParameterTypes());

            MethodCreator forward = clientProxy.getMethodCreator(originalMethodDescriptor);

            // Exceptions
            for (Type exception : method.exceptions()) {
                forward.addException(exception.asClassType().toString());
            }
            // Method params
            ResultHandle[] params = new ResultHandle[method.parameters().size()];
            for (int i = 0; i < method.parameters().size(); ++i) {
                params[i] = forward.getMethodParam(i);
            }

            ResultHandle delegate = forward
                    .invokeVirtualMethod(MethodDescriptor.ofMethod(generatedName, "delegate", DescriptorUtils.typeToString(providerType)), forward.getThis());
            ResultHandle ret;

            if (isInterface) {
                ret = forward.invokeInterfaceMethod(method, delegate, params);
            } else if (isReflectionFallbackNeeded(method, targetPackage)) {
                // Reflection fallback
                ResultHandle paramTypesArray = forward.newArray(Class.class, forward.load(method.parameters().size()));
                int idx = 0;
                for (Type param : method.parameters()) {
                    forward.writeArrayValue(paramTypesArray, idx++, forward.loadClass(param.name().toString()));
                }
                ResultHandle argsArray = forward.newArray(Object.class, forward.load(params.length));
                idx = 0;
                for (ResultHandle argHandle : params) {
                    forward.writeArrayValue(argsArray, idx++, argHandle);
                }
                reflectionRegistration.registerMethod(method);
                ret = forward.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD, forward.loadClass(method.declaringClass().name().toString()),
                        forward.load(method.name()), paramTypesArray, delegate, argsArray);
            } else {
                ret = forward.invokeVirtualMethod(virtualMethod, delegate, params);
            }
            // Finally write the bytecode
            forward.returnValue(ret);
        }

        clientProxy.close();
        return classOutput.getResources();
    }

    void createConstructor(ClassCreator clientProxy, String beanClassName, String superClasName, FieldDescriptor beanField) {
        MethodCreator creator = clientProxy.getMethodCreator(Methods.INIT, void.class, beanClassName);
        creator.invokeSpecialMethod(MethodDescriptor.ofConstructor(superClasName), creator.getThis());
        creator.writeInstanceField(beanField, creator.getThis(), creator.getMethodParam(0));
        creator.returnValue(null);
    }

    void implementDelegate(ClassCreator clientProxy, String providerTypeName, FieldDescriptor beanField) {
        // Arc.container().getContext(bean.getScope()).get(bean, new CreationalContextImpl<>());
        MethodCreator creator = clientProxy.getMethodCreator("delegate", providerTypeName).setModifiers(Modifier.PRIVATE);
        // Arc.container()
        ResultHandle container = creator.invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        // bean.getScope()
        ResultHandle bean = creator.readInstanceField(beanField, creator.getThis());
        ResultHandle scope = creator.invokeInterfaceMethod(MethodDescriptor.ofMethod(InjectableBean.class, "getScope", Class.class), bean);
        // getContext()
        ResultHandle context = creator.invokeInterfaceMethod(MethodDescriptor.ofMethod(ArcContainer.class, "getContext", InjectableContext.class, Class.class),
                container, scope);
        // new CreationalContextImpl<>()
        ResultHandle creationContext = creator.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
        ResultHandle result = creator.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET, context, bean, creationContext);
        creator.returnValue(result);
    }

    void implementGetContextualInstance(ClassCreator clientProxy, String providerTypeName) {
        MethodCreator creator = clientProxy.getMethodCreator("getContextualInstance", Object.class).setModifiers(Modifier.PUBLIC);
        creator.returnValue(
                creator.invokeVirtualMethod(MethodDescriptor.ofMethod(clientProxy.getClassName(), "delegate", providerTypeName), creator.getThis()));
    }

    Collection<MethodInfo> getDelegatingMethods(BeanInfo bean) {
        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();

        if (bean.isClassBean()) {
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), bean.getTarget().get().asClass(), Collections.emptyMap(), methods);
        } else if (bean.isProducerMethod()) {
            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            Map<TypeVariable, Type> resolved = Collections.emptyMap();
            ClassInfo returnTypeClass = bean.getDeployment().getIndex().getClassByName(producerMethod.returnType().name());
            if (!returnTypeClass.typeParameters().isEmpty()) {
                resolved = Types.buildResolvedMap(producerMethod.returnType().asParameterizedType().arguments(), returnTypeClass.typeParameters(),
                        Collections.emptyMap());
            }
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), returnTypeClass, resolved, methods);
        } else if (bean.isProducerField()) {
            FieldInfo producerField = bean.getTarget().get().asField();
            Map<TypeVariable, Type> resolved = Collections.emptyMap();
            ClassInfo fieldClass = bean.getDeployment().getIndex().getClassByName(producerField.type().name());
            if (!fieldClass.typeParameters().isEmpty()) {
                resolved = Types.buildResolvedMap(producerField.type().asParameterizedType().arguments(), fieldClass.typeParameters(), Collections.emptyMap());
            }
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), fieldClass, resolved, methods);
        }
        return methods.values();
    }

}
