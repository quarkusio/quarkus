package org.jboss.protean.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.CreationalContextImpl;
import org.jboss.protean.arc.CurrentInjectionPointProvider;
import org.jboss.protean.arc.InitializedInterceptor;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InjectableInterceptor;
import org.jboss.protean.arc.InjectableReferenceProvider;
import org.jboss.protean.arc.InvocationContextImpl;
import org.jboss.protean.arc.LazyValue;
import org.jboss.protean.arc.Subclass;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.arc.processor.ResourceOutput.Resource.SpecialType;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.ExceptionTable;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.FunctionCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class BeanGenerator extends AbstractGenerator {

    static final String BEAN_SUFFIX = "_Bean";

    static final String PRODUCER_METHOD_SUFFIX = "_ProducerMethod";

    static final String PRODUCER_FIELD_SUFFIX = "_ProducerField";

    private static final Logger LOGGER = Logger.getLogger(BeanGenerator.class);

    private static final AtomicInteger PRODUCER_INDEX = new AtomicInteger();

    private static final String FIELD_NAME_DECLARING_PROVIDER = "declaringProvider";

    /**
     *
     * @param bean
     * @param annotationLiterals
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean, AnnotationLiteralProcessor annotationLiterals) {
        if (Kind.CLASS.equals(bean.getTarget().kind())) {
            return generateClassBean(bean, bean.getTarget().asClass(), annotationLiterals);
        } else if (Kind.METHOD.equals(bean.getTarget().kind())) {
            return generateProducerMethodBean(bean, bean.getTarget().asMethod(), annotationLiterals);
        } else if (Kind.FIELD.equals(bean.getTarget().kind())) {
            return generateProducerFieldBean(bean, bean.getTarget().asField(), annotationLiterals);
        }
        throw new IllegalArgumentException("Unsupported bean type");
    }

    Collection<Resource> generateClassBean(BeanInfo bean, ClassInfo beanClass, AnnotationLiteralProcessor annotationLiterals) {

        String baseName;
        if (beanClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(beanClass.enclosingClass()) + "_" + DotNames.simpleName(beanClass.name());
        } else {
            baseName = DotNames.simpleName(beanClass.name());
        }
        Type providerType = bean.getProviderType();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String generatedName = DotNames.packageName(providerType.name()).replace(".", "/") + "/" + baseName + BEAN_SUFFIX;

        ResourceClassOutput classOutput = new ResourceClassOutput(name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName).interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator("beanTypes", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator("qualifiers", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator("proxy", LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToProviderField = new HashMap<>();
        initMaps(bean, injectionPointToProviderField, interceptorToProviderField);

        createProviderFields(beanCreator, bean, injectionPointToProviderField, interceptorToProviderField);
        createConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, interceptorToProviderField, annotationLiterals);

        if (!bean.hasDefaultDestroy()) {
            createDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderField);
        }
        createCreate(beanCreator, bean, providerTypeName, baseName, injectionPointToProviderField, interceptorToProviderField);
        createGet(bean, beanCreator, providerTypeName);

        createGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!bean.getScope().isDefault()) {
            createGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            createGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        if (bean.isAlternative()) {
            createGetAlternativePriority(bean, beanCreator);
        }

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerMethodBean(BeanInfo bean, MethodInfo producerMethod, AnnotationLiteralProcessor annotationLiterals) {

        ClassInfo declaringClass = producerMethod.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + "_" + DotNames.simpleName(declaringClass.name());
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass.name());
        }

        Type providerType = bean.getProviderType();
        String baseName = declaringClassBase + PRODUCER_METHOD_SUFFIX + PRODUCER_INDEX.incrementAndGet();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String generatedName = DotNames.packageName(declaringClass.name()).replace(".", "/") + "/" + baseName + BEAN_SUFFIX;

        ResourceClassOutput classOutput = new ResourceClassOutput(name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName).interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator("beanTypes", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator("qualifiers", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator("proxy", LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        // Producer methods are not intercepted
        initMaps(bean, injectionPointToProviderField, null);

        createProviderFields(beanCreator, bean, injectionPointToProviderField, Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, Collections.emptyMap(), annotationLiterals);

        if (!bean.hasDefaultDestroy()) {
            createDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderField);
        }
        createCreate(beanCreator, bean, providerTypeName, baseName, injectionPointToProviderField, Collections.emptyMap());
        createGet(bean, beanCreator, providerTypeName);

        createGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!bean.getScope().isDefault()) {
            createGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            createGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        createGetDeclaringBean(beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerFieldBean(BeanInfo bean, FieldInfo producerField, AnnotationLiteralProcessor annotationLiterals) {

        ClassInfo declaringClass = producerField.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + "_" + DotNames.simpleName(declaringClass.name());
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass.name());
        }

        Type providerType = bean.getProviderType();
        String baseName = declaringClassBase + PRODUCER_FIELD_SUFFIX + PRODUCER_INDEX.incrementAndGet();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String generatedName = DotNames.packageName(declaringClass.name()).replace(".", "/") + "/" + baseName + BEAN_SUFFIX;

        ResourceClassOutput classOutput = new ResourceClassOutput(name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName).interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator("beanTypes", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator("qualifiers", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator("proxy", LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        createProviderFields(beanCreator, bean, Collections.emptyMap(), Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, baseName, Collections.emptyMap(), Collections.emptyMap(), annotationLiterals);

        if (!bean.hasDefaultDestroy()) {
            // FIXME!!!
            createDestroy(bean, beanCreator, providerTypeName, null);
        }
        createCreate(beanCreator, bean, providerTypeName, baseName, Collections.emptyMap(), Collections.emptyMap());
        createGet(bean, beanCreator, providerTypeName);

        createGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!bean.getScope().isDefault()) {
            createGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            createGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        createGetDeclaringBean(beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    protected void initMaps(BeanInfo bean, Map<InjectionPointInfo, String> injectionPointToProvider, Map<InterceptorInfo, String> interceptorToProvider) {
        int providerIdx = 1;
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            String name = providerName(DotNames.simpleName(injectionPoint.requiredType.name())) + "Provider" + providerIdx++;
            injectionPointToProvider.put(injectionPoint, name);
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                String name = providerName(DotNames.simpleName(injectionPoint.requiredType.name())) + "Provider" + providerIdx++;
                injectionPointToProvider.put(injectionPoint, name);
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            String name = providerName(DotNames.simpleName(interceptor.getProviderType().name())) + "Provider" + providerIdx++;
            interceptorToProvider.put(interceptor, name);
        }
    }

    protected void createProviderFields(ClassCreator beanCreator, BeanInfo bean, Map<InjectionPointInfo, String> injectionPointToProvider,
            Map<InterceptorInfo, String> interceptorToProvider) {
        // Declaring bean provider
        if (bean.isProducerMethod() || bean.isProducerField()) {
            beanCreator.getFieldCreator(FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        // Injection points
        for (String provider : injectionPointToProvider.values()) {
            beanCreator.getFieldCreator(provider, InjectableReferenceProvider.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        // Interceptors
        for (String interceptorProvider : interceptorToProvider.values()) {
            beanCreator.getFieldCreator(interceptorProvider, InjectableInterceptor.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, Map<InterceptorInfo, String> interceptorToProviderField,
            AnnotationLiteralProcessor annotationLiterals) {
        initConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, interceptorToProviderField, annotationLiterals)
                .returnValue(null);
    }

    protected MethodCreator initConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, Map<InterceptorInfo, String> interceptorToProviderField,
            AnnotationLiteralProcessor annotationLiterals) {

        // First collect all param types
        List<String> parameterTypes = new ArrayList<>();
        if (bean.isProducerMethod() || bean.isProducerField()) {
            parameterTypes.add(InjectableBean.class.getName());
        }
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            if (BuiltinBean.resolve(injectionPoint) == null) {
                parameterTypes.add(InjectableReferenceProvider.class.getName());
            }
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                if (BuiltinBean.resolve(injectionPoint) == null) {
                    parameterTypes.add(InjectableReferenceProvider.class.getName());
                }
            }
        }
        for (int i = 0; i < interceptorToProviderField.size(); i++) {
            parameterTypes.add(InjectableInterceptor.class.getName());
        }

        MethodCreator constructor = beanCreator.getMethodCreator("<init>", "V", parameterTypes.toArray(new String[0]));
        // Invoke super()
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());

        // Declaring bean provider
        int paramIdx = 0;
        if (bean.isProducerMethod() || bean.isProducerField()) {
            constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(0));
            paramIdx++;
        }

        List<InjectionPointInfo> allInjectionPoints = new ArrayList<>();
        allInjectionPoints.addAll(bean.getAllInjectionPoints());
        if (bean.getDisposer() != null) {
            allInjectionPoints.addAll(bean.getDisposer().getInjection().injectionPoints);
        }
        for (InjectionPointInfo injectionPoint : allInjectionPoints) {
            // Injection points
            BuiltinBean builtinBean = null;
            if (injectionPoint.getResolvedBean() == null) {
                builtinBean = BuiltinBean.resolve(injectionPoint);
            }
            if (builtinBean != null) {
                builtinBean.getGenerator().generate(classOutput, bean.getDeployment(), injectionPoint, beanCreator, constructor,
                        injectionPointToProviderField.get(injectionPoint), annotationLiterals);
            } else {
                if (injectionPoint.getResolvedBean().getAllInjectionPoints().stream()
                        .anyMatch(ip -> BuiltinBean.INJECTION_POINT.getRawTypeDotName().equals(ip.requiredType.name()))) {
                    // IMPL NOTE: Injection point resolves to a dependent bean that injects InjectionPoint metadata and so we need to wrap the injectable
                    // reference provider
                    ResultHandle requiredQualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
                    for (AnnotationInstance qualifierAnnotation : injectionPoint.requiredQualifiers) {
                        BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                        if (qualifier != null) {
                            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle, qualifier.getLiteralInstance(constructor));
                        } else {
                            // Create annotation literal first
                            ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifierAnnotation.name());
                            String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                                    Types.getPackageName(beanCreator.getClassName()));
                            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle,
                                    constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                        }
                    }
                    ResultHandle wrapHandle = constructor.newInstance(
                            MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableReferenceProvider.class, java.lang.reflect.Type.class,
                                    Set.class),
                            constructor.getMethodParam(paramIdx++), Types.getTypeHandle(constructor, injectionPoint.requiredType), requiredQualifiersHandle);
                    constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                            InjectableReferenceProvider.class.getName()), constructor.getThis(), wrapHandle);
                } else {
                    constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                            InjectableReferenceProvider.class.getName()), constructor.getThis(), constructor.getMethodParam(paramIdx++));
                }
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(paramIdx++));
        }

        // Bean types
        ResultHandle typesHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (org.jboss.jandex.Type type : bean.getTypes()) {
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, typesHandle, Types.getTypeHandle(constructor, type));
        }
        ResultHandle unmodifiableTypesHandle = constructor
                .invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "unmodifiableSet", Set.class, Set.class), typesHandle);
        constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), "beanTypes", Set.class.getName()), constructor.getThis(),
                unmodifiableTypesHandle);

        // Qualifiers
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {

            ResultHandle qualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

            for (AnnotationInstance qualifierAnnotation : bean.getQualifiers()) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle, qualifier.getLiteralInstance(constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifierAnnotation.name());
                    String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                            Types.getPackageName(beanCreator.getClassName()));
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                }
            }
            ResultHandle unmodifiableQualifiersHandle = constructor
                    .invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "unmodifiableSet", Set.class, Set.class), qualifiersHandle);
            constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), "qualifiers", Set.class.getName()), constructor.getThis(),
                    unmodifiableQualifiersHandle);
        }

        if (bean.getScope().isNormal()) {
            // this.proxy = new LazyValue(() -> new Bar_ClientProxy(this))
            String proxyTypeName = ClientProxyGenerator.getProxyPackageName(bean) + "." + baseName + ClientProxyGenerator.CLIENT_PROXY_SUFFIX;
            FunctionCreator func = constructor.createFunction(Supplier.class);
            BytecodeCreator funcBytecode = func.getBytecode();
            funcBytecode
                    .returnValue(funcBytecode.newInstance(MethodDescriptor.ofConstructor(proxyTypeName, beanCreator.getClassName()), constructor.getThis()));

            ResultHandle proxyHandle = constructor.newInstance(MethodDescriptor.ofConstructor(LazyValue.class, Supplier.class), func.getInstance());
            constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), "proxy", LazyValue.class.getName()), constructor.getThis(),
                    proxyHandle);
        }

        return constructor;
    }

    protected void createDestroy(BeanInfo bean, ClassCreator beanCreator, String providerTypeName,
            Map<InjectionPointInfo, String> injectionPointToProviderField) {

        MethodCreator destroy = beanCreator.getMethodCreator("destroy", void.class, providerTypeName, CreationalContext.class).setModifiers(ACC_PUBLIC);

        if (bean.isClassBean() && !bean.isInterceptor()) {
            // PreDestroy interceptors
            if (!bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()) {
                destroy.invokeInterfaceMethod(MethodDescriptor.ofMethod(Subclass.class, "destroy", void.class), destroy.getMethodParam(0));
            }

            // TODO callbacks may be defined on superclasses
            Optional<MethodInfo> preDestroy = bean.getTarget().asClass().methods().stream().filter(m -> m.hasAnnotation(DotNames.PRE_DESTROY)).findFirst();
            if (preDestroy.isPresent()) {
                // instance.superCoolDestroyCallback()
                destroy.invokeVirtualMethod(MethodDescriptor.of(preDestroy.get()), destroy.getMethodParam(0));
            }
        } else if (bean.getDisposer() != null) {
            // Invoke the disposer method
            // declaringProvider.get(new CreationalContextImpl<>()).dispose()
            MethodInfo disposerMethod = bean.getDisposer().getDisposerMethod();

            ResultHandle declaringProviderHandle = destroy.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()), destroy.getThis());
            ResultHandle ctxHandle = destroy.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle);
            }

            ResultHandle[] referenceHandles = new ResultHandle[disposerMethod.parameters().size()];
            int disposedParamPosition = bean.getDisposer().getDisposedParameter().position();
            Iterator<InjectionPointInfo> injectionPointsIterator = bean.getDisposer().getInjection().injectionPoints.iterator();
            for (int i = 0; i < disposerMethod.parameters().size(); i++) {
                if (i == disposedParamPosition) {
                    referenceHandles[i] = destroy.getMethodParam(0);
                } else {
                    ResultHandle childCtxHandle = destroy.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, ctxHandle);
                    ResultHandle providerHandle = destroy.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                            injectionPointToProviderField.get(injectionPointsIterator.next()), InjectableReferenceProvider.class.getName()), destroy.getThis());
                    ResultHandle referenceHandle = destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtxHandle);
                    referenceHandles[i] = referenceHandle;
                }
            }

            if (Modifier.isPrivate(disposerMethod.flags())) {
                LOGGER.infof("Disposer %s#%s is private - Arc users are encouraged to avoid using private disposers", disposerMethod.declaringClass().name(),
                        disposerMethod.name());
                ResultHandle paramTypesArray = destroy.newArray(Class.class, destroy.load(referenceHandles.length));
                ResultHandle argsArray = destroy.newArray(Object.class, destroy.load(referenceHandles.length));
                for (int i = 0; i < referenceHandles.length; i++) {
                    destroy.writeArrayValue(paramTypesArray, destroy.load(i), destroy.loadClass(disposerMethod.parameters().get(i).name().toString()));
                    destroy.writeArrayValue(argsArray, destroy.load(i), referenceHandles[i]);
                }
                destroy.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD, destroy.loadClass(disposerMethod.declaringClass().name().toString()),
                        destroy.load(disposerMethod.name()), paramTypesArray, declaringProviderInstanceHandle, argsArray);
            } else {
                destroy.invokeVirtualMethod(MethodDescriptor.of(disposerMethod), declaringProviderInstanceHandle, referenceHandles);
            }

            // Destroy @Dependent instances injected into method parameters of a disposer method
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, ctxHandle);

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (ScopeInfo.DEPENDENT.equals(bean.getDisposer().getDeclaringBean().getScope())) {
                destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle, declaringProviderInstanceHandle, ctxHandle);
            }
        }
        // ctx.release()
        destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, destroy.getMethodParam(1));
        destroy.returnValue(null);

        // TODO!
        MethodCreator bridgeDestroy = beanCreator.getMethodCreator("destroy", void.class, Object.class, CreationalContext.class).setModifiers(ACC_PUBLIC);
        bridgeDestroy.returnValue(bridgeDestroy.invokeVirtualMethod(destroy.getMethodDescriptor(), bridgeDestroy.getThis(), bridgeDestroy.getMethodParam(0),
                bridgeDestroy.getMethodParam(1)));
    }

    /**
     *
     * @param beanCreator
     * @param bean
     * @param baseName
     * @param injectionPointToProviderField
     * @param interceptorToProviderField
     * @see Contextual#create()
     */
    protected void createCreate(ClassCreator beanCreator, BeanInfo bean, String providerTypeName, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, Map<InterceptorInfo, String> interceptorToProviderField) {

        MethodCreator create = beanCreator.getMethodCreator("create", providerTypeName, CreationalContext.class).setModifiers(ACC_PUBLIC);
        ResultHandle instanceHandle = null;

        if (bean.isClassBean()) {
            List<Injection> methodInjections = bean.getInjections().stream().filter(i -> i.isMethod() && !i.isConstructor()).collect(Collectors.toList());
            List<Injection> fieldInjections = bean.getInjections().stream().filter(i -> i.isField()).collect(Collectors.toList());

            ResultHandle postConstructsHandle = null;
            ResultHandle aroundConstructsHandle = null;
            Map<InterceptorInfo, ResultHandle> interceptorToWrap = new HashMap<>();

            if (bean.hasLifecycleInterceptors()) {
                // Note that we must share the interceptors instances with the intercepted subclass, if present
                List<InterceptorInfo> postConstructs = bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT);
                List<InterceptorInfo> aroundConstructs = bean.getLifecycleInterceptors(InterceptionType.AROUND_CONSTRUCT);

                // Wrap InjectableInterceptors using InitializedInterceptor
                Set<InterceptorInfo> wraps = new HashSet<>();
                wraps.addAll(aroundConstructs);
                wraps.addAll(postConstructs);
                for (InterceptorInfo interceptor : wraps) {
                    ResultHandle interceptorProvider = create.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()),
                            create.getThis());
                    ResultHandle interceptorInstaceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorProvider,
                            create.getMethodParam(0));
                    ResultHandle wrapHandle = create.invokeStaticMethod(MethodDescriptor.ofMethod(InitializedInterceptor.class, "of",
                            InitializedInterceptor.class, Object.class, InjectableInterceptor.class), interceptorInstaceHandle, interceptorProvider);
                    interceptorToWrap.put(interceptor, wrapHandle);
                }

                if (!postConstructs.isEmpty()) {
                    // postConstructs = new ArrayList<InterceptorInvocation>()
                    postConstructsHandle = create.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : postConstructs) {
                        ResultHandle interceptorHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()), create.getThis());
                        ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, create.getMethodParam(0));
                        ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorHandle,
                                childCtxHandle);
                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(MethodDescriptors.INTERCEPTOR_INVOCATION_POST_CONSTRUCT,
                                interceptorHandle, interceptorInstanceHandle);
                        // postConstructs.add(InterceptorInvocation.postConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                        create.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, postConstructsHandle, interceptorInvocationHandle);
                    }
                }
                if (!aroundConstructs.isEmpty()) {
                    // aroundConstructs = new ArrayList<InterceptorInvocation>()
                    aroundConstructsHandle = create.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : aroundConstructs) {
                        ResultHandle interceptorHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()), create.getThis());
                        ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, create.getMethodParam(0));
                        ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorHandle,
                                childCtxHandle);
                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT,
                                interceptorHandle, interceptorInstanceHandle);
                        // aroundConstructs.add(InterceptorInvocation.aroundConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                        create.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, aroundConstructsHandle, interceptorInvocationHandle);
                    }
                }
            }

            // AroundConstruct lifecycle callback interceptors
            if (!bean.getLifecycleInterceptors(InterceptionType.AROUND_CONSTRUCT).isEmpty()) {
                Optional<Injection> constructorInjection = bean.getConstructorInjection();
                ResultHandle constructorHandle;
                if (constructorInjection.isPresent()) {
                    List<String> paramTypes = constructorInjection.get().injectionPoints.stream().map(i -> i.requiredType.name().toString())
                            .collect(Collectors.toList());
                    ResultHandle[] paramsHandles = new ResultHandle[2];
                    paramsHandles[0] = create.loadClass(providerTypeName);
                    ResultHandle paramsArray = create.newArray(Class.class, create.load(paramTypes.size()));
                    for (ListIterator<String> iterator = paramTypes.listIterator(); iterator.hasNext();) {
                        create.writeArrayValue(paramsArray, create.load(iterator.nextIndex()), create.loadClass(iterator.next()));
                    }
                    paramsHandles[1] = paramsArray;
                    constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR, paramsHandles);
                } else {
                    // constructor = Reflections.findConstructor(Foo.class)
                    ResultHandle[] paramsHandles = new ResultHandle[2];
                    paramsHandles[0] = create.loadClass(providerTypeName);
                    paramsHandles[1] = create.newArray(Class.class, create.load(0));
                    constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR, paramsHandles);
                }

                List<ResultHandle> providerHandles = newProviderHandles(bean, beanCreator, create, injectionPointToProviderField, interceptorToProviderField,
                        interceptorToWrap);

                // Forwarding function
                // Supplier<Object> forward = () -> new SimpleBean_Subclass(ctx,lifecycleInterceptorProvider1)
                FunctionCreator func = create.createFunction(Supplier.class);
                BytecodeCreator funcBytecode = func.getBytecode();
                ResultHandle retHandle = newInstanceHandle(bean, beanCreator, funcBytecode, create, providerTypeName, baseName, providerHandles);
                funcBytecode.returnValue(retHandle);

                // InvocationContextImpl.aroundConstruct(constructor,aroundConstructs,forward).proceed()
                ResultHandle invocationContextHandle = create.invokeStaticMethod(MethodDescriptor.ofMethod(InvocationContextImpl.class, "aroundConstruct",
                        InvocationContextImpl.class, Constructor.class, List.class, Supplier.class), constructorHandle, aroundConstructsHandle,
                        func.getInstance());
                ExceptionTable tryCatch = create.addTryCatch();
                BytecodeCreator exceptionCatch = tryCatch.addCatchClause(Exception.class);
                // throw new RuntimeException(e)
                // TODO existing exception param
                exceptionCatch.throwException(RuntimeException.class, "Error invoking aroundConstructs", exceptionCatch.getMethodParam(0));
                instanceHandle = create.invokeInterfaceMethod(MethodDescriptor.ofMethod(InvocationContext.class, "proceed", Object.class),
                        invocationContextHandle);
                tryCatch.complete();

            } else {
                instanceHandle = newInstanceHandle(bean, beanCreator, create, create, providerTypeName, baseName,
                        newProviderHandles(bean, beanCreator, create, injectionPointToProviderField, interceptorToProviderField, interceptorToWrap));
            }

            // Perform field and initializer injections
            for (Injection fieldInjection : fieldInjections) {
                InjectionPointInfo injectionPoint = fieldInjection.injectionPoints.get(0);
                ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, create.getMethodParam(0));
                ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()), create.getThis());
                ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtxHandle);
                if (Modifier.isPrivate(fieldInjection.target.asField().flags())) {
                    LOGGER.infof("@Inject %s#%s is private - Arc users are encouraged to avoid using private injection fields",
                            fieldInjection.target.asField().declaringClass().name(), fieldInjection.target.asField().name());
                    create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_WRITE_FIELD, create.loadClass(providerTypeName),
                            create.load(fieldInjection.target.asField().name()), instanceHandle, referenceHandle);
                } else {
                    create.writeInstanceField(
                            FieldDescriptor.of(providerTypeName, fieldInjection.target.asField().name(), injectionPoint.requiredType.name().toString()),
                            instanceHandle, referenceHandle);
                }
            }
            for (Injection methodInjection : methodInjections) {
                ResultHandle[] referenceHandles = new ResultHandle[methodInjection.injectionPoints.size()];
                int paramIdx = 0;
                for (InjectionPointInfo injectionPoint : methodInjection.injectionPoints) {
                    ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, create.getMethodParam(0));
                    ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                            injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()), create.getThis());
                    ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtxHandle);
                    referenceHandles[paramIdx++] = referenceHandle;
                }
                create.invokeVirtualMethod(MethodDescriptor.of(methodInjection.target.asMethod()), instanceHandle, referenceHandles);
            }

            // PostConstruct lifecycle callback interceptors
            if (!bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT).isEmpty()) {

                // InvocationContextImpl.postConstruct(instance,postConstructs).proceed()
                ResultHandle invocationContextHandle = create.invokeStaticMethod(
                        MethodDescriptor.ofMethod(InvocationContextImpl.class, "postConstruct", InvocationContextImpl.class, Object.class, List.class),
                        instanceHandle, postConstructsHandle);

                ExceptionTable tryCatch = create.addTryCatch();
                BytecodeCreator exceptionCatch = tryCatch.addCatchClause(Exception.class);
                // throw new RuntimeException(e)
                // TODO existing exception param
                exceptionCatch.throwException(RuntimeException.class, "Error invoking postConstructs", exceptionCatch.getMethodParam(0));
                create.invokeInterfaceMethod(MethodDescriptor.ofMethod(InvocationContext.class, "proceed", Object.class), invocationContextHandle);
                tryCatch.complete();
            }

            // TODO callbacks may be defined on superclasses
            if (!bean.isInterceptor()) {
                Optional<MethodInfo> postConstruct = bean.getTarget().asClass().methods().stream().filter(m -> m.hasAnnotation(DotNames.POST_CONSTRUCT))
                        .findFirst();
                if (postConstruct.isPresent()) {
                    create.invokeVirtualMethod(MethodDescriptor.ofMethod(providerTypeName, postConstruct.get().name(), void.class), instanceHandle);
                }
            }

        } else if (bean.isProducerMethod()) {
            // instance = declaringProvider.get(new CreationalContextImpl<>()).produce()
            ResultHandle declaringProviderHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()), create.getThis());
            ResultHandle ctxHandle = create.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle);
            }

            List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
            ResultHandle[] referenceHandles = new ResultHandle[injectionPoints.size()];
            int paramIdx = 0;
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, create.getMethodParam(0));
                ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()), create.getThis());
                ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtxHandle);
                referenceHandles[paramIdx++] = referenceHandle;
            }

            MethodInfo producerMethod = bean.getTarget().asMethod();
            if (Modifier.isPrivate(producerMethod.flags())) {
                LOGGER.infof("Producer %s#%s is private - Arc users are encouraged to avoid using private producers", producerMethod.declaringClass().name(),
                        producerMethod.name());
                ResultHandle paramTypesArray = create.newArray(Class.class, create.load(referenceHandles.length));
                ResultHandle argsArray = create.newArray(Object.class, create.load(referenceHandles.length));
                for (int i = 0; i < referenceHandles.length; i++) {
                    create.writeArrayValue(paramTypesArray, create.load(i), create.loadClass(producerMethod.parameters().get(i).name().toString()));
                    create.writeArrayValue(argsArray, create.load(i), referenceHandles[i]);
                }
                instanceHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        create.loadClass(producerMethod.declaringClass().name().toString()), create.load(producerMethod.name()), paramTypesArray,
                        declaringProviderInstanceHandle, argsArray);
            } else {
                instanceHandle = create.invokeVirtualMethod(MethodDescriptor.of(producerMethod), declaringProviderInstanceHandle, referenceHandles);
            }

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (ScopeInfo.DEPENDENT.equals(bean.getDeclaringBean().getScope())) {
                create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle, declaringProviderInstanceHandle, ctxHandle);
            }

        } else if (bean.isProducerField()) {
            // instance = declaringProvider.get(new CreationalContextImpl<>()).field

            FieldInfo producerField = bean.getTarget().asField();

            ResultHandle declaringProviderHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()), create.getThis());
            ResultHandle ctxHandle = create.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle);
            }

            if (Modifier.isPrivate(producerField.flags())) {
                LOGGER.infof("Producer %s#%s is private - Arc users are encouraged to avoid using private producers", producerField.declaringClass().name(),
                        producerField.name());
                instanceHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_READ_FIELD,
                        create.loadClass(producerField.declaringClass().name().toString()), create.load(producerField.name()), declaringProviderInstanceHandle);
            } else {
                instanceHandle = create.readInstanceField(FieldDescriptor.of(bean.getTarget().asField()), declaringProviderInstanceHandle);
            }

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (ScopeInfo.DEPENDENT.equals(bean.getDeclaringBean().getScope())) {
                create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle, declaringProviderInstanceHandle, ctxHandle);
            }
        }

        create.returnValue(instanceHandle);

        // Bridge method needed
        MethodCreator bridgeCreate = beanCreator.getMethodCreator("create", Object.class, CreationalContext.class).setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeCreate.returnValue(bridgeCreate.invokeVirtualMethod(create.getMethodDescriptor(), bridgeCreate.getThis(), bridgeCreate.getMethodParam(0)));
    }

    private List<ResultHandle> newProviderHandles(BeanInfo bean, ClassCreator beanCreator, MethodCreator createMethod,
            Map<InjectionPointInfo, String> injectionPointToProviderField, Map<InterceptorInfo, String> interceptorToProviderField,
            Map<InterceptorInfo, ResultHandle> interceptorToWrap) {

        List<ResultHandle> providerHandles = new ArrayList<>();
        Optional<Injection> constructorInjection = bean.getConstructorInjection();

        if (constructorInjection.isPresent()) {
            for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                ResultHandle providerHandle = createMethod.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()), createMethod.getThis());
                ResultHandle childCtx = createMethod.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, createMethod.getMethodParam(0));
                providerHandles.add(createMethod.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtx));
            }
        }
        if (bean.isSubclassRequired()) {
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                ResultHandle wrapped = interceptorToWrap.get(interceptor);
                if (wrapped != null) {
                    providerHandles.add(wrapped);
                } else {
                    providerHandles.add(createMethod.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()),
                            createMethod.getThis()));
                }
            }
        }
        return providerHandles;
    }

    private ResultHandle newInstanceHandle(BeanInfo bean, ClassCreator beanCreator, BytecodeCreator creator, MethodCreator createMethod,
            String providerTypeName, String baseName, List<ResultHandle> providerHandles) {

        Optional<Injection> constructorInjection = bean.getConstructorInjection();
        MethodInfo constructor = constructorInjection.isPresent() ? constructorInjection.get().target.asMethod() : null;
        List<InjectionPointInfo> injectionPoints = constructorInjection.isPresent() ? constructorInjection.get().injectionPoints : Collections.emptyList();

        if (bean.isSubclassRequired()) {
            // new SimpleBean_Subclass(foo,ctx,lifecycleInterceptorProvider1)

            List<InterceptorInfo> interceptors = bean.getBoundInterceptors();
            List<String> paramTypes = new ArrayList<>();
            List<ResultHandle> paramHandles = new ArrayList<>();

            // 1. constructor injection points
            for (int i = 0; i < injectionPoints.size(); i++) {
                paramTypes.add(injectionPoints.get(i).requiredType.name().toString());
                paramHandles.add(providerHandles.get(i));
            }

            // 2. ctx
            paramHandles.add(createMethod.getMethodParam(0));
            paramTypes.add(CreationalContext.class.getName());

            // 3. interceptors (wrapped if needed)
            for (int i = 0; i < interceptors.size(); i++) {
                paramTypes.add(InjectableInterceptor.class.getName());
                paramHandles.add(providerHandles.get(injectionPoints.size() + i));
            }

            return creator.newInstance(
                    MethodDescriptor.ofConstructor(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName), paramTypes.toArray(new String[0])),
                    paramHandles.toArray(new ResultHandle[0]));

        } else if (constructorInjection.isPresent()) {
            if (Modifier.isPrivate(constructor.flags())) {
                LOGGER.infof("Constructor %s is private - Arc users are encouraged to avoid using private interceptor methods",
                        constructor.declaringClass().name());
                ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(providerHandles.size()));
                ResultHandle argsArray = creator.newArray(Object.class, creator.load(providerHandles.size()));
                for (int i = 0; i < injectionPoints.size(); i++) {
                    creator.writeArrayValue(paramTypesArray, creator.load(i), creator.loadClass(injectionPoints.get(i).requiredType.name().toString()));
                    creator.writeArrayValue(argsArray, creator.load(i), providerHandles.get(i));
                }
                return creator.invokeStaticMethod(MethodDescriptors.REFLECTIONS_NEW_INSTANCE, creator.loadClass(constructor.declaringClass().name().toString()),
                        paramTypesArray, argsArray);
            } else {
                // new SimpleBean(foo)
                return creator.newInstance(
                        MethodDescriptor.ofConstructor(providerTypeName,
                                injectionPoints.stream().map(ip -> ip.requiredType.name().toString()).collect(Collectors.toList()).toArray(new String[0])),
                        providerHandles.toArray(new ResultHandle[0]));
            }
        } else {
            MethodInfo noArgsConstructor = bean.getTarget().asClass().method("<init>");
            if (Modifier.isPrivate(noArgsConstructor.flags())) {
                LOGGER.infof("Constructor %s is private - Arc users are encouraged to avoid using private interceptor methods",
                        noArgsConstructor.declaringClass().name());
                ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(0));
                ResultHandle argsArray = creator.newArray(Object.class, creator.load(0));
                return creator.invokeStaticMethod(MethodDescriptors.REFLECTIONS_NEW_INSTANCE,
                        creator.loadClass(noArgsConstructor.declaringClass().name().toString()), paramTypesArray, argsArray);
            } else {
                // new SimpleBean()
                return creator.newInstance(MethodDescriptor.ofConstructor(providerTypeName));
            }
        }
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @param providerTypeName
     * @see InjectableReferenceProvider#get()
     */
    protected void createGet(BeanInfo bean, ClassCreator beanCreator, String providerTypeName) {

        MethodCreator get = beanCreator.getMethodCreator("get", providerTypeName, CreationalContext.class).setModifiers(ACC_PUBLIC);

        if (ScopeInfo.DEPENDENT.equals(bean.getScope())) {
            // Foo instance = create(ctx)
            ResultHandle instance = get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), "create", providerTypeName, CreationalContext.class), get.getThis(),
                    get.getMethodParam(0));
            // CreationalContextImpl.addDependencyToParent(this,instance,ctx)
            get.invokeStaticMethod(MethodDescriptor.ofMethod(CreationalContextImpl.class, "addDependencyToParent", void.class, InjectableBean.class,
                    Object.class, CreationalContext.class), get.getThis(), instance, get.getMethodParam(0));
            // return instance
            get.returnValue(instance);
        } else if (ScopeInfo.SINGLETON.equals(bean.getScope())) {
            // return Arc.container().getContext(getScope()).get(this, new CreationalContextImpl<>())
            ResultHandle container = get.invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
            ResultHandle creationalContext = get.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle scope = get.loadClass(bean.getScope().getClazz());
            ResultHandle context = get.invokeInterfaceMethod(MethodDescriptor.ofMethod(ArcContainer.class, "getContext", Context.class, Class.class), container,
                    scope);
            get.returnValue(get.invokeInterfaceMethod(MethodDescriptor.ofMethod(Context.class, "get", Object.class, Contextual.class, CreationalContext.class),
                    context, get.getThis(), creationalContext));
        } else if (bean.getScope().isNormal()) {
            // return proxy.get()
            ResultHandle proxy = get.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(), "proxy", LazyValue.class.getName()), get.getThis());
            get.returnValue(get.invokeVirtualMethod(MethodDescriptor.ofMethod(LazyValue.class, "get", Object.class), proxy));
        }

        // Bridge method needed
        MethodCreator bridgeGet = beanCreator.getMethodCreator("get", Object.class, CreationalContext.class).setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeGet.returnValue(bridgeGet.invokeVirtualMethod(get.getMethodDescriptor(), bridgeGet.getThis(), bridgeGet.getMethodParam(0)));
    }

    /**
     *
     * @param beanCreator
     * @see InjectableBean#getTypes()
     */
    protected void createGetTypes(ClassCreator beanCreator, FieldDescriptor typesField) {
        MethodCreator getScope = beanCreator.getMethodCreator("getTypes", Set.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope.readInstanceField(typesField, getScope.getThis()));
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @see InjectableBean#getScope()
     */
    protected void createGetScope(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getScope = beanCreator.getMethodCreator("getScope", Class.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope.loadClass(bean.getScope().getClazz()));
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @param qualifiersField
     * @see InjectableBean#getQualifiers()
     */
    protected void createGetQualifiers(BeanInfo bean, ClassCreator beanCreator, FieldDescriptor qualifiersField) {
        MethodCreator getQualifiers = beanCreator.getMethodCreator("getQualifiers", Set.class).setModifiers(ACC_PUBLIC);
        getQualifiers.returnValue(getQualifiers.readInstanceField(qualifiersField, getQualifiers.getThis()));
    }

    protected void createGetDeclaringBean(ClassCreator beanCreator) {
        MethodCreator getDeclaringBean = beanCreator.getMethodCreator("getDeclaringBean", InjectableBean.class).setModifiers(ACC_PUBLIC);
        getDeclaringBean.returnValue(getDeclaringBean.readInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()), getDeclaringBean.getThis()));
    }

    protected void createGetAlternativePriority(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getAlternativePriority = beanCreator.getMethodCreator("getAlternativePriority", Integer.class).setModifiers(ACC_PUBLIC);
        getAlternativePriority.returnValue(getAlternativePriority.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                getAlternativePriority.load(bean.getAlternativePriority())));
    }

}
