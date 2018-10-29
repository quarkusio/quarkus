package org.jboss.protean.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.CreationalContextImpl;
import org.jboss.protean.arc.CurrentInjectionPointProvider;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InjectableObserverMethod;
import org.jboss.protean.arc.InjectableReferenceProvider;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.arc.processor.ResourceOutput.Resource.SpecialType;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class ObserverGenerator extends AbstractGenerator {

    static final String OBSERVER_SUFFIX = "_Observer";

    private static final Logger LOGGER = Logger.getLogger(ObserverGenerator.class);

    private static final AtomicInteger OBSERVER_INDEX = new AtomicInteger();

    private final AnnotationLiteralProcessor annotationLiterals;

    /**
     *
     * @param annotationLiterals
     */
    public ObserverGenerator(AnnotationLiteralProcessor annotationLiterals) {
        this.annotationLiterals = annotationLiterals;
    }

    /**
     *
     * @param observer
     * @return a collection of resources
     */
    Collection<Resource> generate(ObserverInfo observer, ReflectionRegistration reflectionRegistration) {

        ClassInfo declaringClass = observer.getObserverMethod().declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + "_" + DotNames.simpleName(declaringClass.name());
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass.name());
        }

        String baseName = declaringClassBase + OBSERVER_SUFFIX + OBSERVER_INDEX.incrementAndGet();
        String generatedName = DotNames.packageName(declaringClass.name()).replace('.', '/') + "/" + baseName;

        ResourceClassOutput classOutput = new ResourceClassOutput(name -> name.equals(generatedName) ? SpecialType.OBSERVER : null);

        // Foo_Observer1 implements ObserverMethod<T>
        ClassCreator observerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName).interfaces(InjectableObserverMethod.class)
                .build();

        // Fields
        FieldCreator observedType = observerCreator.getFieldCreator("observedType", Type.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator observedQualifiers = null;
        if (!observer.getQualifiers().isEmpty()) {
            observedQualifiers = observerCreator.getFieldCreator("qualifiers", Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        initMaps(observer, injectionPointToProviderField);

        createProviderFields(observerCreator, observer, injectionPointToProviderField);
        createConstructor(classOutput, observerCreator, observer, baseName, injectionPointToProviderField, annotationLiterals);

        implementGetObservedType(observerCreator, observedType.getFieldDescriptor());
        if (observedQualifiers != null) {
            implementGetObservedQualifiers(observerCreator, observedQualifiers.getFieldDescriptor());
        }
        implementGetBeanClass(observerCreator, observer.getDeclaringBean().getTarget().get().asClass().name());
        implementNotify(observer, observerCreator, injectionPointToProviderField, reflectionRegistration);
        if (observer.getPriority() != ObserverMethod.DEFAULT_PRIORITY) {
            implementGetPriority(observerCreator, observer);
        }
        if (observer.isAsync()) {
            implementIsAsync(observerCreator);
        }

        observerCreator.close();
        return classOutput.getResources();
    }

    protected void initMaps(ObserverInfo observer, Map<InjectionPointInfo, String> injectionPointToProvider) {
        int providerIdx = 1;
        for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
            String name = providerName(DotNames.simpleName(injectionPoint.getRequiredType().name())) + "Provider" + providerIdx++;
            injectionPointToProvider.put(injectionPoint, name);
        }
    }

    protected void implementGetObservedType(ClassCreator observerCreator, FieldDescriptor observedTypeField) {
        MethodCreator getObservedType = observerCreator.getMethodCreator("getObservedType", Type.class).setModifiers(ACC_PUBLIC);
        getObservedType.returnValue(getObservedType.readInstanceField(observedTypeField, getObservedType.getThis()));
    }

    protected void implementGetObservedQualifiers(ClassCreator observerCreator, FieldDescriptor observedQualifiersField) {
        MethodCreator getObservedQualifiers = observerCreator.getMethodCreator("getObservedQualifiers", Set.class).setModifiers(ACC_PUBLIC);
        getObservedQualifiers.returnValue(getObservedQualifiers.readInstanceField(observedQualifiersField, getObservedQualifiers.getThis()));
    }

    protected void implementGetBeanClass(ClassCreator observerCreator, DotName beanClass) {
        MethodCreator getBeanClass = observerCreator.getMethodCreator("getBeanClass", Class.class).setModifiers(ACC_PUBLIC);
        getBeanClass.returnValue(getBeanClass.loadClass(beanClass.toString()));
    }

    protected void implementGetPriority(ClassCreator observerCreator, ObserverInfo observer) {
        MethodCreator getPriority = observerCreator.getMethodCreator("getPriority", int.class).setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(observer.getPriority()));
    }

    protected void implementIsAsync(ClassCreator observerCreator) {
        MethodCreator isAsync = observerCreator.getMethodCreator("isAsync", boolean.class).setModifiers(ACC_PUBLIC);
        isAsync.returnValue(isAsync.load(true));
    }

    protected void implementNotify(ObserverInfo observer, ClassCreator observerCreator, Map<InjectionPointInfo, String> injectionPointToProviderField,
            ReflectionRegistration reflectionRegistration) {
        MethodCreator notify = observerCreator.getMethodCreator("notify", void.class, EventContext.class).setModifiers(ACC_PUBLIC);

        ResultHandle declaringProviderHandle = notify
                .readInstanceField(FieldDescriptor.of(observerCreator.getClassName(), "declaringProvider", InjectableBean.class.getName()), notify.getThis());
        ResultHandle ctxHandle = notify.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
        ResultHandle declaringProviderInstanceHandle = notify.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                ctxHandle);

        if (observer.getDeclaringBean().getScope().isNormal()) {
            // We need to unwrap the client proxy
            declaringProviderInstanceHandle = notify.invokeInterfaceMethod(MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                    declaringProviderInstanceHandle);
        }

        ResultHandle[] referenceHandles = new ResultHandle[observer.getObserverMethod().parameters().size()];
        int eventParamPosition = observer.getEventParameter().position();
        Iterator<InjectionPointInfo> injectionPointsIterator = observer.getInjection().injectionPoints.iterator();
        for (int i = 0; i < observer.getObserverMethod().parameters().size(); i++) {
            if (i == eventParamPosition) {
                referenceHandles[i] = notify.invokeInterfaceMethod(MethodDescriptors.EVENT_CONTEXT_GET_EVENT, notify.getMethodParam(0));
            } else if (i == observer.getEventMetadataParameterPosition()) {
                referenceHandles[i] = notify.invokeInterfaceMethod(MethodDescriptors.EVENT_CONTEXT_GET_METADATA, notify.getMethodParam(0));
            } else {
                ResultHandle childCtxHandle = notify.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, ctxHandle);
                ResultHandle providerHandle = notify.readInstanceField(FieldDescriptor.of(observerCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPointsIterator.next()), InjectableReferenceProvider.class.getName()), notify.getThis());
                ResultHandle referenceHandle = notify.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtxHandle);
                referenceHandles[i] = referenceHandle;
            }
        }

        if (Modifier.isPrivate(observer.getObserverMethod().flags())) {
            LOGGER.infof("Observer %s#%s is private - Arc users are encouraged to avoid using private observers",
                    observer.getObserverMethod().declaringClass().name(), observer.getObserverMethod().name());
            ResultHandle paramTypesArray = notify.newArray(Class.class, notify.load(referenceHandles.length));
            ResultHandle argsArray = notify.newArray(Object.class, notify.load(referenceHandles.length));
            for (int i = 0; i < referenceHandles.length; i++) {
                notify.writeArrayValue(paramTypesArray, i, notify.loadClass(observer.getObserverMethod().parameters().get(i).name().toString()));
                notify.writeArrayValue(argsArray, i, referenceHandles[i]);
            }
            reflectionRegistration.registerMethod(observer.getObserverMethod());
            notify.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                    notify.loadClass(observer.getObserverMethod().declaringClass().name().toString()), notify.load(observer.getObserverMethod().name()),
                    paramTypesArray, declaringProviderInstanceHandle, argsArray);
        } else {
            notify.invokeVirtualMethod(MethodDescriptor.of(observer.getObserverMethod()), declaringProviderInstanceHandle, referenceHandles);
        }

        // Destroy @Dependent instances injected into method parameters of a disposer method
        notify.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, ctxHandle);

        // If the declaring bean is @Dependent we must destroy the instance afterwards
        if (ScopeInfo.DEPENDENT.equals(observer.getDeclaringBean().getScope())) {
            notify.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle, declaringProviderInstanceHandle, ctxHandle);
        }

        notify.returnValue(null);
    }

    protected void createProviderFields(ClassCreator observerCreator, ObserverInfo observer, Map<InjectionPointInfo, String> injectionPointToProvider) {
        // Declaring bean provider
        observerCreator.getFieldCreator("declaringProvider", InjectableBean.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        // Injection points
        for (String provider : injectionPointToProvider.values()) {
            observerCreator.getFieldCreator(provider, InjectableReferenceProvider.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator observerCreator, ObserverInfo observer, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, AnnotationLiteralProcessor annotationLiterals) {

        // First collect all param types
        List<String> parameterTypes = new ArrayList<>();
        parameterTypes.add(InjectableBean.class.getName());
        for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
            if (BuiltinBean.resolve(injectionPoint) == null) {
                parameterTypes.add(InjectableReferenceProvider.class.getName());
            }
        }

        MethodCreator constructor = observerCreator.getMethodCreator("<init>", "V", parameterTypes.toArray(new String[0]));
        // Invoke super()
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());

        int paramIdx = 0;
        constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), "declaringProvider", InjectableBean.class.getName()),
                constructor.getThis(), constructor.getMethodParam(0));
        paramIdx++;
        for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
            // Injection points
            BuiltinBean builtinBean = null;
            if (injectionPoint.getResolvedBean() == null) {
                builtinBean = BuiltinBean.resolve(injectionPoint);
            }
            if (builtinBean != null) {
                builtinBean.getGenerator().generate(classOutput, observer.getDeclaringBean().getDeployment(), injectionPoint, observerCreator, constructor,
                        injectionPointToProviderField.get(injectionPoint), annotationLiterals);
            } else {
                if (injectionPoint.getResolvedBean().getAllInjectionPoints().stream()
                        .anyMatch(ip -> BuiltinBean.INJECTION_POINT.getRawTypeDotName().equals(ip.getRequiredType().name()))) {
                    // IMPL NOTE: Injection point resolves to a dependent bean that injects InjectionPoint metadata and so we need to wrap the injectable
                    // reference provider
                    ResultHandle requiredQualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
                    for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
                        BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                        if (qualifier != null) {
                            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle, qualifier.getLiteralInstance(constructor));
                        } else {
                            // Create annotation literal first
                            ClassInfo qualifierClass = observer.getDeclaringBean().getDeployment().getQualifier(qualifierAnnotation.name());
                            String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                                    Types.getPackageName(observerCreator.getClassName()));
                            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle,
                                    constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                        }
                    }
                    ResultHandle wrapHandle = constructor.newInstance(
                            MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableReferenceProvider.class, java.lang.reflect.Type.class,
                                    Set.class),
                            constructor.getMethodParam(paramIdx++), Types.getTypeHandle(constructor, injectionPoint.getRequiredType()), requiredQualifiersHandle);
                    constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                            InjectableReferenceProvider.class.getName()), constructor.getThis(), wrapHandle);
                } else {
                    constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                            InjectableReferenceProvider.class.getName()), constructor.getThis(), constructor.getMethodParam(paramIdx++));
                }
            }
        }

        // Observed type
        constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), "observedType", Type.class.getName()), constructor.getThis(),
                Types.getTypeHandle(constructor, observer.getObservedType()));

        // Qualifiers
        Set<AnnotationInstance> qualifiers = observer.getQualifiers();
        if (!qualifiers.isEmpty()) {

            ResultHandle qualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

            for (AnnotationInstance qualifierAnnotation : qualifiers) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle, qualifier.getLiteralInstance(constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = observer.getDeclaringBean().getDeployment().getQualifier(qualifierAnnotation.name());
                    String annotationLiteralName = annotationLiterals.process(classOutput, qualifierClass, qualifierAnnotation,
                            Types.getPackageName(observerCreator.getClassName()));
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            constructor.newInstance(MethodDescriptor.ofConstructor(annotationLiteralName)));
                }
            }
            ResultHandle unmodifiableQualifiersHandle = constructor
                    .invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "unmodifiableSet", Set.class, Set.class), qualifiersHandle);
            constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), "qualifiers", Set.class.getName()), constructor.getThis(),
                    unmodifiableQualifiersHandle);
        }

        constructor.returnValue(null);
    }

}
