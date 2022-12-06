package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.arc.processor.BeanGenerator.ProviderType;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class ClientProxyGenerator extends AbstractGenerator {

    static final String CLIENT_PROXY_SUFFIX = "_ClientProxy";

    static final String DELEGATE_METHOD_NAME = "arc$delegate";
    static final String SET_MOCK_METHOD_NAME = "arc$setMock";
    static final String CLEAR_MOCK_METHOD_NAME = "arc$clearMock";
    static final String GET_CONTEXTUAL_INSTANCE_METHOD_NAME = "arc_contextualInstance";
    static final String GET_BEAN = "arc_bean";
    static final String BEAN_FIELD = "bean";
    static final String MOCK_FIELD = "mock";
    static final String CONTEXT_FIELD = "context";

    private final Predicate<DotName> applicationClassPredicate;
    private final boolean mockable;
    private final Set<String> existingClasses;

    public ClientProxyGenerator(Predicate<DotName> applicationClassPredicate, boolean generateSources, boolean mockable,
            ReflectionRegistration reflectionRegistration, Set<String> existingClasses) {
        super(generateSources, reflectionRegistration);
        this.applicationClassPredicate = applicationClassPredicate;
        this.mockable = mockable;
        this.existingClasses = existingClasses;
    }

    /**
     *
     * @param bean
     * @param beanClassName Fully qualified class name
     * @param bytecodeTransformerConsumer
     * @param transformUnproxyableClasses whether or not unproxyable classes should be transformed
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean, String beanClassName,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {

        ProviderType providerType = new ProviderType(bean.getProviderType());
        ClassInfo providerClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), providerType.name());
        String baseName = getBaseName(bean, beanClassName);
        String targetPackage = bean.getClientProxyPackageName();
        String generatedName = generatedNameFromTarget(targetPackage, baseName, CLIENT_PROXY_SUFFIX);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean applicationClass = applicationClassPredicate.test(getApplicationClassTestName(bean));
        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClass,
                name -> name.equals(generatedName) ? SpecialType.CLIENT_PROXY : null, generateSources);

        // Foo_ClientProxy extends Foo implements ClientProxy
        List<String> interfaces = new ArrayList<>();
        String superClass = Object.class.getName();
        interfaces.add(ClientProxy.class.getName());
        boolean isInterface = false;

        if (Modifier.isInterface(providerClass.flags())) {
            isInterface = true;
            interfaces.add(providerType.className());
        } else {
            superClass = providerType.className();
        }
        if (mockable) {
            interfaces.add(Mockable.class.getName());
        }

        ClassCreator clientProxy = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(superClass)
                .interfaces(interfaces.toArray(new String[0])).build();
        // See https://docs.oracle.com/javase/specs/jvms/se14/html/jvms-4.html#jvms-4.7.9.1
        // Essentially a signature is needed if a class has type parameters or extends/implements parameterized type.
        // We're generating a subtype (subclass or subinterface) of "providerClass".
        // The only way for that generated subtype to have type parameters or to extend/implement a parameterized type
        // is if providerClass has type parameters.
        // Whether supertypes or superinterfaces of providerClass have type parameters is irrelevant:
        // as long as those type parameters are bound in providerClass,
        // they won't affect the need for a signature in the generated subtype.
        if (!providerClass.typeParameters().isEmpty()) {
            clientProxy.setSignature(AsmUtilCopy.getGeneratedSubClassSignature(providerClass, bean.getProviderType()));
        }
        Map<ClassInfo, Map<String, Type>> resolvedTypeVariables = Types.resolvedTypeVariables(providerClass,
                bean.getDeployment());
        FieldCreator beanField = clientProxy.getFieldCreator(BEAN_FIELD, InjectableBean.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        if (mockable) {
            clientProxy.getFieldCreator(MOCK_FIELD, providerType.descriptorName()).setModifiers(ACC_PRIVATE | ACC_VOLATILE);
        }
        FieldCreator contextField = null;
        if (BuiltinScope.APPLICATION.is(bean.getScope())) {
            // It is safe to store the application context instance on the proxy
            contextField = clientProxy.getFieldCreator(CONTEXT_FIELD, InjectableContext.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        createConstructor(clientProxy, superClass, beanField.getFieldDescriptor(),
                contextField != null ? contextField.getFieldDescriptor() : null);
        implementDelegate(clientProxy, providerType, beanField.getFieldDescriptor(), bean);
        implementGetContextualInstance(clientProxy, providerType);
        implementGetBean(clientProxy, beanField.getFieldDescriptor());
        if (mockable) {
            implementMockMethods(clientProxy, providerType);
        }

        for (MethodInfo method : getDelegatingMethods(bean, bytecodeTransformerConsumer, transformUnproxyableClasses)) {

            MethodDescriptor originalMethodDescriptor = MethodDescriptor.of(method);
            MethodCreator forward = clientProxy.getMethodCreator(originalMethodDescriptor);
            if (AsmUtilCopy.needsSignature(method)) {
                Map<String, Type> methodClassVariables = resolvedTypeVariables.get(method.declaringClass());
                String signature = AsmUtilCopy.getSignature(method, typeVariable -> {
                    if (methodClassVariables != null) {
                        Type ret = methodClassVariables.get(typeVariable.identifier());
                        // let's not map a TV to itself
                        if (ret != typeVariable)
                            return ret;
                    }
                    return null;
                });
                forward.setSignature(signature);
            }

            // Exceptions
            for (Type exception : method.exceptions()) {
                forward.addException(exception.toString());
            }
            // Method params
            ResultHandle[] params = new ResultHandle[method.parametersCount()];
            for (int i = 0; i < method.parametersCount(); ++i) {
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
                            method.parameterTypes().stream().map(p -> p.name().toString()).toArray());
                    notConstructed.returnValue(
                            notConstructed.invokeSpecialMethod(superDescriptor, notConstructed.getThis(), params));
                }
            }

            ResultHandle delegate = forward
                    .invokeVirtualMethod(
                            MethodDescriptor.ofMethod(generatedName, DELEGATE_METHOD_NAME,
                                    providerType.descriptorName()),
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
                // make sure we invoke the method upon the provider type, i.e. don't use the original method descriptor
                MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerType.className(),
                        originalMethodDescriptor.getName(),
                        originalMethodDescriptor.getReturnType(),
                        originalMethodDescriptor.getParameterTypes());
                ret = forward.invokeInterfaceMethod(virtualMethod, delegate, params);
            } else if (isReflectionFallbackNeeded(method, targetPackage)) {
                // Reflection fallback
                ResultHandle paramTypesArray = forward.newArray(Class.class, forward.load(method.parametersCount()));
                int idx = 0;
                for (Type param : method.parameterTypes()) {
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
                MethodDescriptor virtualMethod = MethodDescriptor.ofMethod(providerType.className(),
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

    private void implementMockMethods(ClassCreator clientProxy, ProviderType providerType) {
        MethodCreator clear = clientProxy
                .getMethodCreator(MethodDescriptor.ofMethod(clientProxy.getClassName(), CLEAR_MOCK_METHOD_NAME, void.class));
        clear.writeInstanceField(FieldDescriptor.of(clientProxy.getClassName(), MOCK_FIELD, providerType.descriptorName()),
                clear.getThis(),
                clear.loadNull());
        clear.returnValue(null);

        MethodCreator set = clientProxy
                .getMethodCreator(
                        MethodDescriptor.ofMethod(clientProxy.getClassName(), SET_MOCK_METHOD_NAME, void.class, Object.class));
        set.writeInstanceField(FieldDescriptor.of(clientProxy.getClassName(), MOCK_FIELD, providerType.descriptorName()),
                set.getThis(),
                set.getMethodParam(0));
        set.returnValue(null);
    }

    void createConstructor(ClassCreator clientProxy, String superClasName, FieldDescriptor beanField,
            FieldDescriptor contextField) {
        MethodCreator creator = clientProxy.getMethodCreator(Methods.INIT, void.class, String.class);
        creator.invokeSpecialMethod(MethodDescriptor.ofConstructor(superClasName), creator.getThis());
        ResultHandle containerHandle = creator.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
        ResultHandle beanIdentifierHandle = creator.getMethodParam(0);
        ResultHandle beanHandle = creator.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_BEAN, containerHandle,
                beanIdentifierHandle);
        creator.writeInstanceField(beanField, creator.getThis(), beanHandle);
        if (contextField != null) {
            creator.writeInstanceField(contextField, creator.getThis(), creator.invokeInterfaceMethod(
                    MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                    containerHandle, creator
                            .invokeInterfaceMethod(MethodDescriptor.ofMethod(InjectableBean.class, "getScope", Class.class),
                                    beanHandle)));
        }
        creator.returnValue(null);
    }

    void implementDelegate(ClassCreator clientProxy, ProviderType providerType, FieldDescriptor beanField, BeanInfo bean) {
        MethodCreator creator = clientProxy.getMethodCreator(DELEGATE_METHOD_NAME, providerType.descriptorName())
                .setModifiers(Modifier.PRIVATE);
        if (mockable) {
            //if mockable and mocked just return the mock
            ResultHandle mock = creator.readInstanceField(
                    FieldDescriptor.of(clientProxy.getClassName(), MOCK_FIELD, providerType.descriptorName()),
                    creator.getThis());
            BytecodeCreator falseBranch = creator.ifNull(mock).falseBranch();
            falseBranch.returnValue(mock);
        }

        ResultHandle beanHandle = creator.readInstanceField(beanField, creator.getThis());

        if (BuiltinScope.APPLICATION.is(bean.getScope())) {
            // Application context is stored in a field and is always active
            creator.returnValue(creator.invokeStaticMethod(MethodDescriptors.CLIENT_PROXIES_GET_APP_SCOPED_DELEGATE,
                    creator.readInstanceField(
                            FieldDescriptor.of(clientProxy.getClassName(), CONTEXT_FIELD, InjectableContext.class),
                            creator.getThis()),
                    beanHandle));
        } else {
            creator.returnValue(creator.invokeStaticMethod(MethodDescriptors.CLIENT_PROXIES_GET_DELEGATE,
                    beanHandle));
        }
    }

    void implementGetContextualInstance(ClassCreator clientProxy, ProviderType providerType) {
        MethodCreator creator = clientProxy.getMethodCreator(GET_CONTEXTUAL_INSTANCE_METHOD_NAME, Object.class)
                .setModifiers(Modifier.PUBLIC);
        creator.returnValue(
                creator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(clientProxy.getClassName(), DELEGATE_METHOD_NAME,
                                providerType.descriptorName()),
                        creator.getThis()));
    }

    void implementGetBean(ClassCreator clientProxy, FieldDescriptor beanField) {
        MethodCreator creator = clientProxy.getMethodCreator(GET_BEAN, InjectableBean.class)
                .setModifiers(Modifier.PUBLIC);
        creator.returnValue(creator.readInstanceField(beanField, creator.getThis()));
    }

    Collection<MethodInfo> getDelegatingMethods(BeanInfo bean, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();
        IndexView index = bean.getDeployment().getBeanArchiveIndex();

        if (bean.isClassBean()) {
            Map<String, Set<Methods.NameAndDescriptor>> methodsFromWhichToRemoveFinal = new HashMap<>();
            ClassInfo classInfo = bean.getTarget().get().asClass();
            addDelegatesAndTrasformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, classInfo);
        } else if (bean.isProducerMethod()) {
            Map<String, Set<Methods.NameAndDescriptor>> methodsFromWhichToRemoveFinal = new HashMap<>();
            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            ClassInfo returnTypeClass = getClassByName(index, producerMethod.returnType());
            addDelegatesAndTrasformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, returnTypeClass);
        } else if (bean.isProducerField()) {
            Map<String, Set<Methods.NameAndDescriptor>> methodsFromWhichToRemoveFinal = new HashMap<>();
            FieldInfo producerField = bean.getTarget().get().asField();
            ClassInfo fieldClass = getClassByName(index, producerField.type());
            addDelegatesAndTrasformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, fieldClass);
        } else if (bean.isSynthetic()) {
            Methods.addDelegatingMethods(index, bean.getImplClazz(), methods, null,
                    transformUnproxyableClasses);
        }

        return methods.values();
    }

    private void addDelegatesAndTrasformIfNecessary(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses,
            Map<Methods.MethodKey, MethodInfo> methods, IndexView index,
            Map<String, Set<Methods.NameAndDescriptor>> methodsFromWhichToRemoveFinal,
            ClassInfo fieldClass) {
        Methods.addDelegatingMethods(index, fieldClass, methods, methodsFromWhichToRemoveFinal,
                transformUnproxyableClasses);
        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            for (Map.Entry<String, Set<Methods.NameAndDescriptor>> entry : methodsFromWhichToRemoveFinal.entrySet()) {
                String className = entry.getKey();
                bytecodeTransformerConsumer.accept(new BytecodeTransformer(className,
                        new Methods.RemoveFinalFromMethod(className, entry.getValue())));
            }
        }
    }

    private DotName getApplicationClassTestName(BeanInfo bean) {
        DotName testedName;
        // For producers we need to test the produced type
        if (bean.isProducerField()) {
            testedName = bean.getTarget().get().asField().type().name();
        } else if (bean.isProducerMethod()) {
            testedName = bean.getTarget().get().asMethod().returnType().name();
        } else {
            testedName = bean.getBeanClass();
        }
        return testedName;
    }

}
