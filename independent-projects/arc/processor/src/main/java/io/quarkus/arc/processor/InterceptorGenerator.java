package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

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
     *
     * @param interceptor bean
     * @return a collection of resources
     */
    Collection<Resource> generate(InterceptorInfo interceptor) {

        ProviderType providerType = new ProviderType(interceptor.getProviderType());
        ClassInfo interceptorClass = interceptor.getTarget().get().asClass();
        String baseName;
        if (interceptorClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(interceptorClass.enclosingClass()) + "_" + DotNames.simpleName(interceptorClass);
        } else {
            baseName = DotNames.simpleName(interceptorClass);
        }
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        beanToGeneratedName.put(interceptor, generatedName);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(interceptor.getBeanClass());
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
                bindings.getFieldDescriptor(), reflectionRegistration);

        implementGetIdentifier(interceptor, interceptorCreator);
        implementSupplierGet(interceptorCreator);
        implementCreate(classOutput, interceptorCreator, interceptor, providerType, baseName,
                injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(),
                reflectionRegistration, targetPackage, isApplicationClass);
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

    protected void createConstructor(ClassOutput classOutput, ClassCreator creator, InterceptorInfo interceptor,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            FieldDescriptor bindings, ReflectionRegistration reflectionRegistration) {

        MethodCreator constructor = initConstructor(classOutput, creator, interceptor, injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(), annotationLiterals, reflectionRegistration);

        // Bindings
        // bindings = new HashSet<>()
        ResultHandle bindingsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

        for (AnnotationInstance bindingAnnotation : interceptor.getBindings()) {
            // Create annotation literal first
            ClassInfo bindingClass = interceptor.getDeployment().getInterceptorBinding(bindingAnnotation.name());
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                    annotationLiterals.process(constructor, classOutput, bindingClass, bindingAnnotation,
                            Types.getPackageName(creator.getClassName())));
        }
        constructor.writeInstanceField(bindings, constructor.getThis(), bindingsHandle);
        constructor.returnValue(null);
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
     * @see InjectableInterceptor#intercepts(javax.enterprise.inject.spi.InterceptionType)
     */
    protected void implementIntercepts(ClassCreator creator, InterceptorInfo interceptor) {
        MethodCreator intercepts = creator.getMethodCreator("intercepts", boolean.class, InterceptionType.class)
                .setModifiers(ACC_PUBLIC);
        addIntercepts(interceptor, InterceptionType.AROUND_INVOKE, intercepts);
        addIntercepts(interceptor, InterceptionType.POST_CONSTRUCT, intercepts);
        addIntercepts(interceptor, InterceptionType.PRE_DESTROY, intercepts);
        addIntercepts(interceptor, InterceptionType.AROUND_CONSTRUCT, intercepts);
        intercepts.returnValue(intercepts.load(false));
    }

    private void addIntercepts(InterceptorInfo interceptor, InterceptionType interceptionType, MethodCreator intercepts) {
        if (interceptor.intercepts(interceptionType)) {
            ResultHandle enumValue = intercepts
                    .readStaticField(FieldDescriptor.of(InterceptionType.class.getName(), interceptionType.name(),
                            InterceptionType.class.getName()));
            BranchResult result = intercepts
                    .ifNonZero(intercepts.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, enumValue,
                            intercepts.getMethodParam(0)));
            result.trueBranch().returnValue(result.trueBranch().load(true));
        }
    }

    /**
     *
     * @see InjectableInterceptor#intercept(InterceptionType, Object, javax.interceptor.InvocationContext)
     */
    protected void implementIntercept(ClassCreator creator, InterceptorInfo interceptor, ProviderType providerType,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {
        MethodCreator intercept = creator
                .getMethodCreator("intercept", Object.class, InterceptionType.class, Object.class, InvocationContext.class)
                .setModifiers(ACC_PUBLIC).addException(Exception.class);

        addIntercept(intercept, interceptor.getAroundInvoke(), InterceptionType.AROUND_INVOKE, providerType,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getPostConstruct(), InterceptionType.POST_CONSTRUCT, providerType,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getPreDestroy(), InterceptionType.PRE_DESTROY, providerType,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getAroundConstruct(), InterceptionType.AROUND_CONSTRUCT, providerType,
                reflectionRegistration, isApplicationClass);
        intercept.returnValue(intercept.loadNull());
    }

    private void addIntercept(MethodCreator intercept, MethodInfo interceptorMethod, InterceptionType interceptionType,
            ProviderType providerType,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {
        if (interceptorMethod != null) {
            ResultHandle enumValue = intercept
                    .readStaticField(FieldDescriptor.of(InterceptionType.class.getName(), interceptionType.name(),
                            InterceptionType.class.getName()));
            BranchResult result = intercept.ifNonZero(
                    intercept.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, enumValue, intercept.getMethodParam(0)));
            BytecodeCreator trueBranch = result.trueBranch();
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
            if (interceptorMethod.parameters().get(0).name().equals(DotNames.INVOCATION_CONTEXT)) {
                invocationContextClass = InvocationContext.class;
            } else {
                invocationContextClass = ArcInvocationContext.class;
            }
            if (Modifier.isPrivate(interceptorMethod.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Interceptor method %s#%s()", interceptorMethod.declaringClass().name(),
                                interceptorMethod.name()));
                // Use reflection fallback
                ResultHandle paramTypesArray = trueBranch.newArray(Class.class, trueBranch.load(1));
                trueBranch.writeArrayValue(paramTypesArray, 0, trueBranch.loadClass(invocationContextClass));
                ResultHandle argsArray = trueBranch.newArray(Object.class, trueBranch.load(1));
                trueBranch.writeArrayValue(argsArray, 0, intercept.getMethodParam(2));
                reflectionRegistration.registerMethod(interceptorMethod);
                ret = trueBranch.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        trueBranch.loadClass(interceptorMethod.declaringClass()
                                .name()
                                .toString()),
                        trueBranch.load(interceptorMethod.name()), paramTypesArray, intercept.getMethodParam(1), argsArray);
            } else {
                ret = trueBranch.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(providerType.className(), interceptorMethod.name(), retType,
                                invocationContextClass),
                        intercept.getMethodParam(1), intercept.getMethodParam(2));
            }
            trueBranch.returnValue(InterceptionType.AROUND_INVOKE.equals(interceptionType) ? ret : trueBranch.loadNull());
        }
    }
}
