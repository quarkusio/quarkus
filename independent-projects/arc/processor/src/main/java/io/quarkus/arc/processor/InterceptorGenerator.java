package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InterceptorCreator.InterceptFunction;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class InterceptorGenerator extends BeanGenerator {

    protected static final String FIELD_NAME_BINDINGS = "bindings";

    public InterceptorGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(annotationLiterals, applicationClassPredicate, privateMembers, generateSources, reflectionRegistration,
                existingClasses, beanToGeneratedName, injectionPointAnnotationsPredicate, Collections.emptyList());
    }

    /**
     * Precompute the generated name for the given interceptor so that the {@link ComponentsProviderGenerator} can be executed
     * before all interceptors metadata are generated.
     *
     * @param interceptor
     */
    void precomputeGeneratedName(InterceptorInfo interceptor) {
        ProviderType providerType = new ProviderType(interceptor.getProviderType());
        String baseName;
        String targetPackage;
        if (interceptor.isSynthetic()) {
            DotName creatorClassName = DotName.createSimple(interceptor.getCreatorClass());
            baseName = InterceptFunction.class.getSimpleName() + "_" + interceptor.getIdentifier();
            targetPackage = DotNames.packageName(creatorClassName);
        } else {
            ClassInfo interceptorClass = interceptor.getTarget().get().asClass();
            if (interceptorClass.enclosingClass() != null) {
                baseName = DotNames.simpleName(interceptorClass.enclosingClass()) + "_" + DotNames.simpleName(interceptorClass);
            } else {
                baseName = DotNames.simpleName(interceptorClass);
            }
            targetPackage = DotNames.packageName(providerType.name());
        }
        beanToGeneratedBaseName.put(interceptor, baseName);
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        beanToGeneratedName.put(interceptor, generatedName);
    }

    /**
     *
     * @param interceptor bean
     * @return a collection of resources
     */
    Collection<Resource> generate(InterceptorInfo interceptor) {
        return interceptor.isSynthetic() ? generateSyntheticInterceptor(interceptor) : generateClassInterceptor(interceptor);
    }

    private Collection<Resource> generateSyntheticInterceptor(InterceptorInfo interceptor) {
        ProviderType providerType = new ProviderType(interceptor.getProviderType());
        DotName creatorClassName = DotName.createSimple(interceptor.getCreatorClass());
        String baseName = beanToGeneratedBaseName.get(interceptor);
        String targetPackage = DotNames.packageName(creatorClassName);
        String generatedName = beanToGeneratedName.get(interceptor);

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(creatorClassName) || interceptor.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.INTERCEPTOR_BEAN : null, generateSources);

        // MyInterceptor_Bean implements InjectableInterceptor<T>
        ClassCreator interceptorBean = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableInterceptor.class, Supplier.class)
                .build();

        // Fields
        FieldCreator beanTypes = interceptorBean.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator bindings = interceptorBean.getFieldCreator(FIELD_NAME_BINDINGS, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        initMaps(interceptor, injectionPointToProviderField, Collections.emptyMap(), Collections.emptyMap());

        createProviderFields(interceptorBean, interceptor, injectionPointToProviderField, Collections.emptyMap(),
                Collections.emptyMap());
        MethodCreator constructor = createConstructor(classOutput, interceptorBean, interceptor, injectionPointToProviderField,
                bindings.getFieldDescriptor(), reflectionRegistration, isApplicationClass, providerType);
        SyntheticComponentsUtil.addParamsFieldAndInit(interceptorBean, constructor, interceptor.getParams(), annotationLiterals,
                interceptor.getDeployment().getBeanArchiveIndex());
        constructor.returnValue(null);

        implementGetIdentifier(interceptor, interceptorBean);
        implementSupplierGet(interceptorBean);
        implementCreate(classOutput, interceptorBean, interceptor, providerType, baseName,
                injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(),
                targetPackage, isApplicationClass);
        implementGet(interceptor, interceptorBean, providerType, baseName);
        implementGetTypes(interceptorBean, beanTypes.getFieldDescriptor());
        implementGetBeanClass(interceptor, interceptorBean);
        // Interceptors are always @Dependent and have always default qualifiers

        // InjectableInterceptor methods
        implementGetInterceptorBindings(interceptorBean, bindings.getFieldDescriptor());
        implementIntercepts(interceptorBean, interceptor);
        implementIntercept(interceptorBean, interceptor, providerType, reflectionRegistration, isApplicationClass);
        implementGetPriority(interceptorBean, interceptor);

        implementEquals(interceptor, interceptorBean);
        implementHashCode(interceptor, interceptorBean);

        interceptorBean.close();
        return classOutput.getResources();
    }

    private Collection<Resource> generateClassInterceptor(InterceptorInfo interceptor) {
        ProviderType providerType = new ProviderType(interceptor.getProviderType());
        String baseName = beanToGeneratedBaseName.get(interceptor);
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = beanToGeneratedName.get(interceptor);

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(interceptor.getBeanClass())
                || interceptor.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.INTERCEPTOR_BEAN : null, generateSources);

        // MyInterceptor_Bean implements InjectableInterceptor<T>
        ClassCreator interceptorCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableInterceptor.class, Supplier.class)
                .build();

        // Fields
        FieldCreator beanTypes = interceptorCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator bindings = interceptorCreator.getFieldCreator(FIELD_NAME_BINDINGS, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        initMaps(interceptor, injectionPointToProviderField, Collections.emptyMap(), Collections.emptyMap());

        createProviderFields(interceptorCreator, interceptor, injectionPointToProviderField, Collections.emptyMap(),
                Collections.emptyMap());
        createConstructor(classOutput, interceptorCreator, interceptor, injectionPointToProviderField,
                bindings.getFieldDescriptor(), reflectionRegistration, isApplicationClass, providerType).returnValue(null);

        implementGetIdentifier(interceptor, interceptorCreator);
        implementSupplierGet(interceptorCreator);
        implementCreate(classOutput, interceptorCreator, interceptor, providerType, baseName,
                injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(),
                targetPackage, isApplicationClass);
        implementGet(interceptor, interceptorCreator, providerType, baseName);
        implementGetTypes(interceptorCreator, beanTypes.getFieldDescriptor());
        implementGetBeanClass(interceptor, interceptorCreator);
        // Interceptors are always @Dependent and have always default qualifiers

        // InjectableInterceptor methods
        implementGetInterceptorBindings(interceptorCreator, bindings.getFieldDescriptor());
        implementIntercepts(interceptorCreator, interceptor);
        implementIntercept(interceptorCreator, interceptor, providerType, reflectionRegistration, isApplicationClass);
        implementGetPriority(interceptorCreator, interceptor);

        implementEquals(interceptor, interceptorCreator);
        implementHashCode(interceptor, interceptorCreator);

        interceptorCreator.close();
        return classOutput.getResources();
    }

    protected MethodCreator createConstructor(ClassOutput classOutput, ClassCreator creator, InterceptorInfo interceptor,
            Map<InjectionPointInfo, String> injectionPointToProviderField, FieldDescriptor bindings,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass, ProviderType providerType) {

        MethodCreator constructor = initConstructor(classOutput, creator, interceptor, injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(), annotationLiterals, reflectionRegistration);

        // Bindings
        // bindings = new HashSet<>()
        ResultHandle bindingsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

        for (AnnotationInstance bindingAnnotation : interceptor.getBindings()) {
            // Create annotation literal first
            ClassInfo bindingClass = interceptor.getDeployment().getInterceptorBinding(bindingAnnotation.name());
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                    annotationLiterals.create(constructor, bindingClass, bindingAnnotation));
        }
        constructor.writeInstanceField(bindings, constructor.getThis(), bindingsHandle);

        // Initialize a list of BiFunction for each interception type if multiple interceptor methods are declared in a hierarchy
        initInterceptorMethodsField(creator, constructor, InterceptionType.AROUND_INVOKE, interceptor.getAroundInvokes(),
                providerType.className(), isApplicationClass);
        initInterceptorMethodsField(creator, constructor, InterceptionType.AROUND_CONSTRUCT, interceptor.getAroundConstructs(),
                providerType.className(), isApplicationClass);
        initInterceptorMethodsField(creator, constructor, InterceptionType.POST_CONSTRUCT, interceptor.getPostConstructs(),
                providerType.className(), isApplicationClass);
        initInterceptorMethodsField(creator, constructor, InterceptionType.PRE_DESTROY, interceptor.getPreDestroys(),
                providerType.className(), isApplicationClass);

        return constructor;
    }

    private void initInterceptorMethodsField(ClassCreator creator, MethodCreator constructor, InterceptionType interceptionType,
            List<MethodInfo> methods, String interceptorClass, boolean isApplicationClass) {
        if (methods.size() < 2) {
            return;
        }
        FieldCreator field = creator.getFieldCreator(interceptorMethodsField(interceptionType), List.class)
                .setModifiers(ACC_PRIVATE);
        ResultHandle methodsList = constructor.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
        for (MethodInfo method : methods) {
            // BiFunction<Object,InvocationContext,Object>
            FunctionCreator fun = constructor.createFunction(BiFunction.class);
            BytecodeCreator funBytecode = fun.getBytecode();
            ResultHandle ret = invokeInterceptorMethod(funBytecode, interceptorClass, method,
                    interceptionType, isApplicationClass, funBytecode.getMethodParam(1),
                    funBytecode.getMethodParam(0));
            funBytecode.returnValue(interceptionType == InterceptionType.AROUND_INVOKE ? ret : funBytecode.loadNull());
            constructor.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, methodsList, fun.getInstance());
        }
        constructor.writeInstanceField(field.getFieldDescriptor(), constructor.getThis(), methodsList);
    }

    protected void implementGetBeanClass(InterceptorInfo interceptor, ClassCreator beanCreator) {
        MethodCreator getBeanClass = beanCreator.getMethodCreator("getBeanClass", Class.class).setModifiers(ACC_PUBLIC);
        getBeanClass.returnValue(getBeanClass.loadClass(
                interceptor.isSynthetic() ? interceptor.getCreatorClass().getName() : interceptor.getBeanClass().toString()));
    }

    /**
     *
     * @see InjectableInterceptor#getInterceptorBindings()
     */
    protected void implementGetInterceptorBindings(ClassCreator creator, FieldDescriptor bindingsField) {
        MethodCreator getBindings = creator.getMethodCreator("getInterceptorBindings", Set.class).setModifiers(ACC_PUBLIC);
        getBindings.returnValue(getBindings.readInstanceField(bindingsField, getBindings.getThis()));
    }

    /**
     *
     * @see InjectableInterceptor#getPriority()
     */
    protected void implementGetPriority(ClassCreator creator, InterceptorInfo interceptor) {
        MethodCreator getPriority = creator.getMethodCreator("getPriority", int.class).setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(interceptor.getPriority()));
    }

    /**
     *
     * @return the method
     * @see InjectableInterceptor#intercepts(jakarta.enterprise.inject.spi.InterceptionType)
     */
    protected void implementIntercepts(ClassCreator creator, InterceptorInfo interceptor) {
        MethodCreator intercepts = creator.getMethodCreator("intercepts", boolean.class, InterceptionType.class)
                .setModifiers(ACC_PUBLIC);
        if (interceptor.isSynthetic()) {
            ResultHandle enumValue = intercepts
                    .readStaticField(
                            FieldDescriptor.of(InterceptionType.class.getName(), interceptor.getInterceptionType().name(),
                                    InterceptionType.class.getName()));
            BranchResult result = intercepts
                    .ifTrue(Gizmo.equals(intercepts, enumValue, intercepts.getMethodParam(0)));
            result.trueBranch().returnValue(result.trueBranch().load(true));
        } else {
            addIntercepts(interceptor, InterceptionType.AROUND_INVOKE, intercepts);
            addIntercepts(interceptor, InterceptionType.POST_CONSTRUCT, intercepts);
            addIntercepts(interceptor, InterceptionType.PRE_DESTROY, intercepts);
            addIntercepts(interceptor, InterceptionType.AROUND_CONSTRUCT, intercepts);
        }
        intercepts.returnValue(intercepts.load(false));
    }

    private void addIntercepts(InterceptorInfo interceptor, InterceptionType interceptionType, MethodCreator intercepts) {
        if (interceptor.intercepts(interceptionType)) {
            ResultHandle enumValue = intercepts
                    .readStaticField(FieldDescriptor.of(InterceptionType.class.getName(), interceptionType.name(),
                            InterceptionType.class.getName()));
            BranchResult result = intercepts
                    .ifTrue(Gizmo.equals(intercepts, enumValue, intercepts.getMethodParam(0)));
            result.trueBranch().returnBoolean(true);
        }
    }

    /**
     *
     * @see InjectableInterceptor#intercept(InterceptionType, Object, jakarta.interceptor.InvocationContext)
     */
    protected void implementIntercept(ClassCreator creator, InterceptorInfo interceptor, ProviderType providerType,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {
        MethodCreator intercept = creator
                .getMethodCreator("intercept", Object.class, InterceptionType.class, Object.class, InvocationContext.class)
                .setModifiers(ACC_PUBLIC).addException(Exception.class);

        if (interceptor.isSynthetic()) {
            BranchResult result = intercept
                    .ifTrue(Gizmo.equals(intercept, intercept.load(interceptor.getInterceptionType()),
                            intercept.getMethodParam(0)));
            BytecodeCreator trueBranch = result.trueBranch();
            ResultHandle interceptFunction = trueBranch.checkCast(trueBranch.getMethodParam(1), InterceptFunction.class);
            trueBranch.returnValue(trueBranch.invokeInterfaceMethod(MethodDescriptors.INTERCEPT_FUNCTION_INTERCEPT,
                    interceptFunction, trueBranch.getMethodParam(2)));
        } else {
            addIntercept(creator, intercept, interceptor.getAroundInvokes(), InterceptionType.AROUND_INVOKE, providerType,
                    reflectionRegistration, isApplicationClass);
            addIntercept(creator, intercept, interceptor.getPostConstructs(), InterceptionType.POST_CONSTRUCT, providerType,
                    reflectionRegistration, isApplicationClass);
            addIntercept(creator, intercept, interceptor.getPreDestroys(), InterceptionType.PRE_DESTROY, providerType,
                    reflectionRegistration, isApplicationClass);
            addIntercept(creator, intercept, interceptor.getAroundConstructs(), InterceptionType.AROUND_CONSTRUCT, providerType,
                    reflectionRegistration, isApplicationClass);
        }
        intercept.returnValue(intercept.loadNull());
    }

    private void addIntercept(ClassCreator creator, MethodCreator intercept, List<MethodInfo> interceptorMethods,
            InterceptionType interceptionType, ProviderType providerType, ReflectionRegistration reflectionRegistration,
            boolean isApplicationClass) {
        if (interceptorMethods.isEmpty()) {
            return;
        }
        BranchResult result = intercept
                .ifTrue(Gizmo.equals(intercept, intercept.load(interceptionType), intercept.getMethodParam(0)));
        BytecodeCreator trueBranch = result.trueBranch();
        ResultHandle ret;
        if (interceptorMethods.size() == 1) {
            MethodInfo interceptorMethod = interceptorMethods.get(0);
            ret = invokeInterceptorMethod(trueBranch, providerType.className(), interceptorMethod,
                    interceptionType, isApplicationClass, trueBranch.getMethodParam(2), trueBranch.getMethodParam(1));
        } else {
            // Multiple interceptor methods found in the hierarchy
            ResultHandle methodList = trueBranch.readInstanceField(
                    FieldDescriptor.of(creator.getClassName(), interceptorMethodsField(interceptionType), List.class),
                    trueBranch.getThis());
            ResultHandle params;
            if (interceptionType == InterceptionType.AROUND_INVOKE) {
                params = trueBranch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_GET_PARAMETERS,
                        trueBranch.getMethodParam(2));
            } else {
                params = trueBranch.loadNull();
            }
            ret = trueBranch.invokeStaticMethod(MethodDescriptors.INVOCATION_CONTEXTS_PERFORM_SUPERCLASS,
                    trueBranch.getMethodParam(2), methodList,
                    trueBranch.getMethodParam(1), params);

        }
        trueBranch.returnValue(InterceptionType.AROUND_INVOKE.equals(interceptionType) ? ret : trueBranch.loadNull());
    }

    private String interceptorMethodsField(InterceptionType interceptionType) {
        switch (interceptionType) {
            case AROUND_INVOKE:
                return "aroundInvokes";
            case AROUND_CONSTRUCT:
                return "aroundConstructs";
            case POST_CONSTRUCT:
                return "postConstructs";
            case PRE_DESTROY:
                return "preDestroys";
            default:
                throw new IllegalArgumentException("Unsupported interception type: " + interceptionType);
        }
    }

    private ResultHandle invokeInterceptorMethod(BytecodeCreator creator, String interceptorClass, MethodInfo interceptorMethod,
            InterceptionType interceptionType, boolean isApplicationClass, ResultHandle invocationContext,
            ResultHandle interceptorInstance) {
        Class<?> retType = null;
        if (InterceptionType.AROUND_INVOKE.equals(interceptionType)) {
            retType = Object.class;
        } else {
            // @PostConstruct, @PreDestroy, @AroundConstruct
            retType = interceptorMethod.returnType().kind().equals(Type.Kind.VOID) ? void.class : Object.class;
        }
        ResultHandle ret;
        // Check if interceptor method uses InvocationContext or ArcInvocationContext
        Class<?> invocationContextClass;
        if (interceptorMethod.parameterType(0).name().equals(DotNames.INVOCATION_CONTEXT)) {
            invocationContextClass = InvocationContext.class;
        } else {
            invocationContextClass = ArcInvocationContext.class;
        }
        if (Modifier.isPrivate(interceptorMethod.flags())) {
            privateMembers.add(isApplicationClass,
                    String.format("Interceptor method %s#%s()", interceptorMethod.declaringClass().name(),
                            interceptorMethod.name()));
            // Use reflection fallback
            ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(1));
            creator.writeArrayValue(paramTypesArray, 0, creator.loadClass(invocationContextClass));
            ResultHandle argsArray = creator.newArray(Object.class, creator.load(1));
            creator.writeArrayValue(argsArray, 0, invocationContext);
            reflectionRegistration.registerMethod(interceptorMethod);
            ret = creator.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                    creator.loadClass(interceptorMethod.declaringClass()
                            .name()
                            .toString()),
                    creator.load(interceptorMethod.name()), paramTypesArray, interceptorInstance, argsArray);
        } else {
            ret = creator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(interceptorClass, interceptorMethod.name(), retType,
                            invocationContextClass),
                    interceptorInstance, invocationContext);
        }
        return ret;
    }
}
