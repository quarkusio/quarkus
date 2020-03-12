package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class ClientProxyGenerator extends AbstractGenerator {

    static final String CLIENT_PROXY_SUFFIX = "_ClientProxy";

    static final String DELEGATE_METHOD_NAME = "arc$delegate";
    static final String GET_CONTEXTUAL_INSTANCE_METHOD_NAME = "arc_contextualInstance";
    static final String GET_BEAN = "arc_bean";

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
        ClassInfo providerClass = getClassByName(bean.getDeployment().getIndex(), providerType.name());
        String providerTypeName = providerClass.name().toString();
        String baseName = getBaseName(bean, beanClassName);
        String targetPackage = getPackageName(bean);
        String generatedName = generatedNameFromTarget(targetPackage, baseName, CLIENT_PROXY_SUFFIX);

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

        ClassCreator clientProxy = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(superClass)
                .interfaces(interfaces.toArray(new String[0])).build();
        FieldCreator beanField = clientProxy.getFieldCreator("bean", DescriptorUtils.extToInt(beanClassName))
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        createConstructor(clientProxy, beanClassName, superClass, beanField.getFieldDescriptor());
        implementDelegate(clientProxy, providerTypeName, beanField.getFieldDescriptor());
        implementGetContextualInstance(clientProxy, providerTypeName);
        implementGetBean(clientProxy, beanField.getFieldDescriptor());

        for (MethodInfo method : getDelegatingMethods(bean)) {

            MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
            MethodCreator forward = clientProxy.getMethodCreator(originalMethodDescriptor);

            // Exceptions
            for (Type exception : method.exceptions()) {
                forward.addException(exception.toString());
            }
            // Method params
            ResultHandle[] params = new ResultHandle[method.parameters().size()];
            for (int i = 0; i < method.parameters().size(); ++i) {
                params[i] = forward.getMethodParam(i);
            }

            if (!superClass.equals(Object.class.getName())) {
                // Skip delegation if proxy is not constructed yet
                // This check is unnecessary for producers that return an interface
                // if(!this.bean == null) return super.foo()
                BytecodeCreator notConstructed = forward
                        .ifNull(forward.readInstanceField(beanField.getFieldDescriptor(), forward.getThis())).trueBranch();
                if (Modifier.isAbstract(method.flags())) {
                    notConstructed.throwException(IllegalStateException.class, "Cannot delegate to an abstract method");
                } else {
                    MethodDescriptor superDescriptor = MethodDescriptor.ofMethod(superClass, method.name(),
                            method.returnType().name().toString(),
                            method.parameters().stream().map(p -> p.name().toString()).toArray());
                    notConstructed.returnValue(
                            notConstructed.invokeSpecialMethod(superDescriptor, notConstructed.getThis(), params));
                }
            }

            ResultHandle delegate = forward
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(generatedName, DELEGATE_METHOD_NAME,
                                    DescriptorUtils.typeToString(providerType)),
                            forward.getThis());
            ResultHandle ret;

            /**
             * Note that we don't have to check for default interface methods if this is an interface,
             * as it just works, and the reflection case cannot be true since it's not possible to have
             * non-public default interface methods.
             */
            if (Methods.isObjectToString(method)) {
                // Always use invokevirtual and the original descriptor for java.lang.Object#toString()
                ret = forward.invokeVirtualMethod(originalMethodDescriptor, delegate, params);
            } else if (isInterface) {
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
                ret = forward.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        forward.loadClass(method.declaringClass().name().toString()),
                        forward.load(method.name()), paramTypesArray, delegate, argsArray);
            } else {
                // make sure we do not use the original method descriptor as it could point to
                // a default interface method containing class: make sure we invoke it on the provider type.
                MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerTypeName,
                        originalMethodDescriptor.getName(),
                        originalMethodDescriptor.getReturnType(),
                        originalMethodDescriptor.getParameterTypes());
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
        MethodCreator creator = clientProxy.getMethodCreator(DELEGATE_METHOD_NAME, providerTypeName)
                .setModifiers(Modifier.PRIVATE);
        // Arc.container()
        ResultHandle container = creator.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
        // bean.getScope()
        ResultHandle bean = creator.readInstanceField(beanField, creator.getThis());
        ResultHandle scope = creator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(InjectableBean.class, "getScope", Class.class), bean);
        // getContext()
        ResultHandle context = creator.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                container, scope);
        BytecodeCreator inactiveBranch = creator.ifNull(context).trueBranch();
        ResultHandle exception = inactiveBranch.newInstance(
                MethodDescriptor.ofConstructor(ContextNotActiveException.class, String.class),
                inactiveBranch.invokeVirtualMethod(MethodDescriptors.OBJECT_TO_STRING, scope));
        inactiveBranch.throwException(exception);
        AssignableResultHandle ret = creator.createVariable(Object.class);
        creator.assign(ret, creator.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET_IF_PRESENT, context, bean));
        BytecodeCreator isNullBranch = creator.ifNull(ret).trueBranch();
        // Create a new contextual instance - new CreationalContextImpl<>()
        ResultHandle creationContext = isNullBranch
                .newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), bean);
        isNullBranch.assign(ret,
                isNullBranch.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET, context, bean, creationContext));
        creator.returnValue(ret);
    }

    void implementGetContextualInstance(ClassCreator clientProxy, String providerTypeName) {
        MethodCreator creator = clientProxy.getMethodCreator(GET_CONTEXTUAL_INSTANCE_METHOD_NAME, Object.class)
                .setModifiers(Modifier.PUBLIC);
        creator.returnValue(
                creator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(clientProxy.getClassName(), DELEGATE_METHOD_NAME, providerTypeName),
                        creator.getThis()));
    }

    void implementGetBean(ClassCreator clientProxy, FieldDescriptor beanField) {
        MethodCreator creator = clientProxy.getMethodCreator(GET_BEAN, InjectableBean.class)
                .setModifiers(Modifier.PUBLIC);
        creator.returnValue(creator.readInstanceField(beanField, creator.getThis()));
    }

    Collection<MethodInfo> getDelegatingMethods(BeanInfo bean) {
        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();

        if (bean.isClassBean()) {
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), bean.getTarget().get().asClass(),
                    methods);
        } else if (bean.isProducerMethod()) {
            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            ClassInfo returnTypeClass = getClassByName(bean.getDeployment().getIndex(), producerMethod.returnType());
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), returnTypeClass, methods);
        } else if (bean.isProducerField()) {
            FieldInfo producerField = bean.getTarget().get().asField();
            ClassInfo fieldClass = getClassByName(bean.getDeployment().getIndex(), producerField.type());
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), fieldClass, methods);
        } else if (bean.isSynthetic()) {
            Methods.addDelegatingMethods(bean.getDeployment().getIndex(), bean.getImplClazz(), methods);
        }
        return methods.values();
    }

}
