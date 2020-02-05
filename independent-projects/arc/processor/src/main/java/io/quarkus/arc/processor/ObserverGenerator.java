package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.CurrentInjectionPointProvider;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.reflect.Member;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.ObserverMethod;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 *
 * @author Martin Kouba
 */
public class ObserverGenerator extends AbstractGenerator {

    static final String OBSERVER_SUFFIX = "_Observer";
    static final String OBSERVERVED_TYPE = "observedType";
    static final String QUALIFIERS = "qualifiers";

    private final AnnotationLiteralProcessor annotationLiterals;

    private final Predicate<DotName> applicationClassPredicate;

    private final PrivateMembersCollector privateMembers;

    public ObserverGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers) {
        this.annotationLiterals = annotationLiterals;
        this.applicationClassPredicate = applicationClassPredicate;
        this.privateMembers = privateMembers;
    }

    /**
     *
     * @param observer
     * @return a collection of resources
     */
    Collection<Resource> generate(ObserverInfo observer, ReflectionRegistration reflectionRegistration) {
        // The name of the generated class differs:
        // "org.acme.Foo_Observer_fooMethod_hash" for normal observer where hash represents the signature of the observer method
        // "org.acme.Registrar_Observer_Synthetic_hash" for synthetic observer where hash represents the basic attrs of the observer
        String classBase;
        if (observer.isSynthetic()) {
            classBase = DotNames.simpleName(observer.getBeanClass());
        } else {
            ClassInfo declaringClass = observer.getObserverMethod().declaringClass();
            if (declaringClass.enclosingClass() != null) {
                classBase = DotNames.simpleName(declaringClass.enclosingClass()) + UNDERSCORE
                        + DotNames.simpleName(declaringClass);
            } else {
                classBase = DotNames.simpleName(declaringClass);
            }
        }

        StringBuilder sigBuilder = new StringBuilder();
        if (observer.isSynthetic()) {
            // This is not unique but best effort
            sigBuilder.append(observer.getObservedType().toString()).append(observer.getQualifiers().toString())
                    .append(observer.isAsync()).append(observer.getPriority()).append(observer.getTransactionPhase());
        } else {
            sigBuilder.append(observer.getObserverMethod().name())
                    .append(UNDERSCORE)
                    .append(observer.getObserverMethod().returnType().name().toString());
            for (org.jboss.jandex.Type paramType : observer.getObserverMethod().parameters()) {
                sigBuilder.append(paramType.name().toString());
            }
        }

        StringBuilder baseName = new StringBuilder();
        baseName.append(classBase).append(OBSERVER_SUFFIX).append(UNDERSCORE);
        if (observer.isSynthetic()) {
            baseName.append(SYNTHETIC_SUFFIX);
        } else {
            baseName.append(observer.getObserverMethod().name());
        }
        baseName.append(UNDERSCORE).append(Hashes.sha1(sigBuilder.toString()));

        // No suffix added at the end of generated name because it's already
        // included in a baseName, e.g. Foo_Observer_fooMethod_hash

        String targetPackage = DotNames.packageName(observer.getBeanClass());
        String generatedName = generatedNameFromTarget(targetPackage, baseName.toString(), "");

        boolean isApplicationClass = applicationClassPredicate.test(observer.getBeanClass());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.OBSERVER : null);

        // Foo_Observer_fooMethod_hash implements ObserverMethod<T>
        ClassCreator observerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableObserverMethod.class)
                .build();

        // Fields
        FieldCreator observedType = observerCreator.getFieldCreator(OBSERVERVED_TYPE, Type.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator observedQualifiers = null;
        if (!observer.getQualifiers().isEmpty()) {
            observedQualifiers = observerCreator.getFieldCreator(QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        initMaps(observer, injectionPointToProviderField);

        createProviderFields(observerCreator, observer, injectionPointToProviderField);
        createConstructor(classOutput, observerCreator, observer, baseName.toString(), injectionPointToProviderField,
                annotationLiterals, reflectionRegistration);

        implementGetObservedType(observerCreator, observedType.getFieldDescriptor());
        if (observedQualifiers != null) {
            implementGetObservedQualifiers(observerCreator, observedQualifiers.getFieldDescriptor());
        }

        if (!observer.getTransactionPhase().equals(TransactionPhase.IN_PROGRESS)) {
            implementGetTransactionPhase(observerCreator, observer);
        }

        implementGetBeanClass(observerCreator, observer.getBeanClass());
        implementNotify(observer, observerCreator, injectionPointToProviderField, reflectionRegistration, isApplicationClass);
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
        if (observer.isSynthetic()) {
            return;
        }
        int providerIdx = 1;
        for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
            if (injectionPoint.getRequiredType().name().equals(DotNames.EVENT_METADATA)) {
                // We do not need a provider for event metadata
                continue;
            }
            injectionPointToProvider.put(injectionPoint, "observerProviderSupplier" + providerIdx++);
        }
    }

    protected void implementGetObservedType(ClassCreator observerCreator, FieldDescriptor observedTypeField) {
        MethodCreator getObservedType = observerCreator.getMethodCreator("getObservedType", Type.class)
                .setModifiers(ACC_PUBLIC);
        getObservedType.returnValue(getObservedType.readInstanceField(observedTypeField, getObservedType.getThis()));
    }

    protected void implementGetObservedQualifiers(ClassCreator observerCreator, FieldDescriptor observedQualifiersField) {
        MethodCreator getObservedQualifiers = observerCreator.getMethodCreator("getObservedQualifiers", Set.class)
                .setModifiers(ACC_PUBLIC);
        getObservedQualifiers
                .returnValue(getObservedQualifiers.readInstanceField(observedQualifiersField, getObservedQualifiers.getThis()));
    }

    protected void implementGetTransactionPhase(ClassCreator observerCreator, ObserverInfo observer) {
        MethodCreator getTransactionPhase = observerCreator.getMethodCreator("getTransactionPhase", TransactionPhase.class)
                .setModifiers(ACC_PUBLIC);
        getTransactionPhase.returnValue(getTransactionPhase.load(observer.getTransactionPhase()));
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

    protected void implementNotify(ObserverInfo observer, ClassCreator observerCreator,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {

        MethodCreator notify = observerCreator.getMethodCreator("notify", void.class, EventContext.class)
                .setModifiers(ACC_PUBLIC);

        if (observer.isSynthetic()) {
            // Synthetic observers generate the notify method themselves 
            observer.getNotify().accept(notify);
            return;
        }

        AssignableResultHandle declaringProviderInstanceHandle = notify.createVariable(Object.class);
        AssignableResultHandle ctxHandle = notify.createVariable(CreationalContextImpl.class);
        ResultHandle declaringProviderSupplierHandle = notify.readInstanceField(
                FieldDescriptor.of(observerCreator.getClassName(), "declaringProviderSupplier",
                        Supplier.class.getName()),
                notify.getThis());
        ResultHandle declaringProviderHandle = notify.invokeInterfaceMethod(
                MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle);
        // It is safe to skip CreationalContext.release() for normal scoped declaring provider and no injection points
        boolean skipRelease = observer.getDeclaringBean().getScope().isNormal()
                && observer.getInjection().injectionPoints.isEmpty();

        // If Reception.IF_EXISTS is used we must check the context of the declaring bean first
        if (Reception.IF_EXISTS == observer.getReception()) {
            BeanInfo declaringBean = observer.getDeclaringBean();
            if (declaringBean != null && !BuiltinScope.DEPENDENT.is(declaringBean.getScope())) {
                ResultHandle container = notify.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
                ResultHandle scope = notify.loadClass(declaringBean.getScope().getDotName().toString());
                ResultHandle context = notify.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                        container,
                        scope);
                notify.ifNull(context).trueBranch().returnValue(null);
                notify.assign(declaringProviderInstanceHandle,
                        notify.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET_IF_PRESENT, context,
                                declaringProviderHandle));
                BranchResult doesNotExist = notify.ifNull(declaringProviderInstanceHandle);
                doesNotExist.trueBranch().returnValue(null);
                BytecodeCreator isNotPresent = doesNotExist.falseBranch();
                isNotPresent.assign(ctxHandle, skipRelease ? isNotPresent.loadNull()
                        : isNotPresent.newInstance(
                                MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                                declaringProviderHandle));
            }
        } else {
            notify.assign(ctxHandle, skipRelease ? notify.loadNull()
                    : notify.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                            declaringProviderHandle));
            notify.assign(declaringProviderInstanceHandle, notify.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle));
            if (observer.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                notify.assign(declaringProviderInstanceHandle, notify.invokeInterfaceMethod(
                        MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle));
            }
        }

        ResultHandle[] referenceHandles = new ResultHandle[observer.getObserverMethod().parameters().size()];
        int eventParamPosition = observer.getEventParameter().position();
        Iterator<InjectionPointInfo> injectionPointsIterator = observer.getInjection().injectionPoints.iterator();
        for (int i = 0; i < observer.getObserverMethod().parameters().size(); i++) {
            if (i == eventParamPosition) {
                referenceHandles[i] = notify.invokeInterfaceMethod(MethodDescriptors.EVENT_CONTEXT_GET_EVENT,
                        notify.getMethodParam(0));
            } else if (i == observer.getEventMetadataParameterPosition()) {
                referenceHandles[i] = notify.invokeInterfaceMethod(MethodDescriptors.EVENT_CONTEXT_GET_METADATA,
                        notify.getMethodParam(0));
            } else {
                ResultHandle childCtxHandle = notify.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD, ctxHandle);
                ResultHandle providerSupplierHandle = notify
                        .readInstanceField(FieldDescriptor.of(observerCreator.getClassName(),
                                injectionPointToProviderField.get(injectionPointsIterator.next()),
                                Supplier.class.getName()), notify.getThis());
                ResultHandle providerHandle = notify.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                        providerSupplierHandle);
                ResultHandle referenceHandle = notify.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle);
                referenceHandles[i] = referenceHandle;
            }
        }

        if (Modifier.isPrivate(observer.getObserverMethod().flags())) {
            privateMembers.add(isApplicationClass,
                    String.format("Observer method %s#%s()", observer.getObserverMethod().declaringClass().name(),
                            observer.getObserverMethod().name()));
            ResultHandle paramTypesArray = notify.newArray(Class.class, notify.load(referenceHandles.length));
            ResultHandle argsArray = notify.newArray(Object.class, notify.load(referenceHandles.length));
            for (int i = 0; i < referenceHandles.length; i++) {
                notify.writeArrayValue(paramTypesArray, i,
                        notify.loadClass(observer.getObserverMethod().parameters().get(i).name().toString()));
                notify.writeArrayValue(argsArray, i, referenceHandles[i]);
            }
            reflectionRegistration.registerMethod(observer.getObserverMethod());
            notify.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                    notify.loadClass(observer.getObserverMethod().declaringClass().name().toString()),
                    notify.load(observer.getObserverMethod().name()),
                    paramTypesArray, declaringProviderInstanceHandle, argsArray);
        } else {
            notify.invokeVirtualMethod(MethodDescriptor.of(observer.getObserverMethod()), declaringProviderInstanceHandle,
                    referenceHandles);
        }

        // Destroy @Dependent instances injected into method parameters of an observer method
        if (!skipRelease) {
            notify.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, ctxHandle);
        }

        // If the declaring bean is @Dependent we must destroy the instance afterwards
        if (BuiltinScope.DEPENDENT.is(observer.getDeclaringBean().getScope())) {
            notify.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                    declaringProviderInstanceHandle, ctxHandle);
        }

        notify.returnValue(null);
    }

    protected void createProviderFields(ClassCreator observerCreator, ObserverInfo observer,
            Map<InjectionPointInfo, String> injectionPointToProvider) {
        // Declaring bean provider
        observerCreator.getFieldCreator("declaringProviderSupplier", Supplier.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        // Injection points
        for (String provider : injectionPointToProvider.values()) {
            observerCreator.getFieldCreator(provider, Supplier.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator observerCreator, ObserverInfo observer,
            String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {

        MethodCreator constructor;
        if (observer.isSynthetic()) {
            constructor = observerCreator.getMethodCreator(Methods.INIT, "V");
            // Invoke super()
            constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());
        } else {
            // Declaring provider and injection points
            // First collect all param types
            List<String> parameterTypes = new ArrayList<>();
            parameterTypes.add(Supplier.class.getName());
            for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
                if (BuiltinBean.resolve(injectionPoint) == null) {
                    parameterTypes.add(Supplier.class.getName());
                }
            }

            constructor = observerCreator.getMethodCreator(Methods.INIT, "V", parameterTypes.toArray(new String[0]));
            // Invoke super()
            constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());

            int paramIdx = 0;
            constructor.writeInstanceField(
                    FieldDescriptor.of(observerCreator.getClassName(), "declaringProviderSupplier", Supplier.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(0));
            paramIdx++;
            for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
                // Injection points
                BuiltinBean builtinBean = null;
                if (injectionPoint.getResolvedBean() == null) {
                    builtinBean = BuiltinBean.resolve(injectionPoint);
                }
                if (builtinBean != null) {
                    builtinBean.getGenerator()
                            .generate(new GeneratorContext(classOutput, observer.getDeclaringBean().getDeployment(),
                                    injectionPoint, observerCreator, constructor,
                                    injectionPointToProviderField.get(injectionPoint),
                                    annotationLiterals, observer, reflectionRegistration));
                } else {
                    if (injectionPoint.getResolvedBean().getAllInjectionPoints().stream()
                            .anyMatch(ip -> BuiltinBean.INJECTION_POINT.hasRawTypeDotName(ip.getRequiredType().name()))) {
                        // IMPL NOTE: Injection point resolves to a dependent bean that injects InjectionPoint metadata and so we need to wrap the injectable
                        // reference provider
                        ResultHandle requiredQualifiersHandle = BeanGenerator.collectQualifiers(classOutput, observerCreator,
                                observer.getDeclaringBean().getDeployment(), constructor,
                                injectionPoint,
                                annotationLiterals);
                        ResultHandle annotationsHandle = BeanGenerator.collectAnnotations(classOutput, observerCreator,
                                observer.getDeclaringBean().getDeployment(), constructor,
                                injectionPoint, annotationLiterals);
                        ResultHandle javaMemberHandle = BeanGenerator.getJavaMemberHandle(constructor, injectionPoint,
                                reflectionRegistration);

                        // Wrap the constructor arg in a Supplier so we can pass it to CurrentInjectionPointProvider c'tor.
                        ResultHandle delegateSupplier = constructor.newInstance(
                                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, constructor.getMethodParam(paramIdx++));

                        ResultHandle wrapHandle = constructor.newInstance(
                                MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableBean.class,
                                        Supplier.class, java.lang.reflect.Type.class,
                                        Set.class, Set.class, Member.class, int.class),
                                constructor.getThis(), delegateSupplier,
                                Types.getTypeHandle(constructor, injectionPoint.getRequiredType()),
                                requiredQualifiersHandle, annotationsHandle, javaMemberHandle,
                                constructor.load(injectionPoint.getPosition()));

                        constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(),
                                injectionPointToProviderField.get(injectionPoint),
                                Supplier.class.getName()), constructor.getThis(), wrapHandle);
                    } else {
                        constructor.writeInstanceField(
                                FieldDescriptor.of(observerCreator.getClassName(),
                                        injectionPointToProviderField.get(injectionPoint),
                                        Supplier.class.getName()),
                                constructor.getThis(), constructor.getMethodParam(paramIdx++));
                    }
                }
            }
        }

        // Observed type
        constructor.writeInstanceField(FieldDescriptor.of(observerCreator.getClassName(), "observedType", Type.class.getName()),
                constructor.getThis(),
                Types.getTypeHandle(constructor, observer.getObservedType()));

        // Qualifiers
        Set<AnnotationInstance> qualifiers = observer.getQualifiers();
        if (!qualifiers.isEmpty()) {

            ResultHandle qualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

            for (AnnotationInstance qualifierAnnotation : qualifiers) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            qualifier.getLiteralInstance(constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = observer.getBeanDeployment()
                            .getQualifier(qualifierAnnotation.name());
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            annotationLiterals.process(constructor, classOutput,
                                    qualifierClass, qualifierAnnotation, Types.getPackageName(observerCreator.getClassName())));
                }
            }
            ResultHandle unmodifiableQualifiersHandle = constructor
                    .invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "unmodifiableSet", Set.class, Set.class),
                            qualifiersHandle);
            constructor.writeInstanceField(
                    FieldDescriptor.of(observerCreator.getClassName(), "qualifiers", Set.class.getName()),
                    constructor.getThis(),
                    unmodifiableQualifiersHandle);
        }

        constructor.returnValue(null);
    }

}
