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

package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
            PrivateMembersCollector privateMembers) {
        super(annotationLiterals, applicationClassPredicate, privateMembers);
    }

    /**
     *
     * @param interceptor bean
     * @param reflectionRegistration
     * @return a collection of resources
     */
    Collection<Resource> generate(InterceptorInfo interceptor, ReflectionRegistration reflectionRegistration) {

        Type providerType = interceptor.getProviderType();
        ClassInfo interceptorClass = interceptor.getTarget().get().asClass();
        String baseName;
        if (interceptorClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(interceptorClass.enclosingClass()) + "_" + DotNames.simpleName(interceptorClass);
        } else {
            baseName = DotNames.simpleName(interceptorClass);
        }
        ClassInfo providerClass = interceptor.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(interceptor.getBeanClass());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.INTERCEPTOR_BEAN : null);

        // MyInterceptor_Bean implements InjectableInterceptor<T>
        ClassCreator interceptorCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableInterceptor.class)
                .build();

        // Fields
        FieldCreator beanTypes = interceptorCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator bindings = interceptorCreator.getFieldCreator(FIELD_NAME_BINDINGS, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToProviderField = new HashMap<>();
        initMaps(interceptor, injectionPointToProviderField, interceptorToProviderField);

        createProviderFields(interceptorCreator, interceptor, injectionPointToProviderField, interceptorToProviderField);
        createConstructor(classOutput, interceptorCreator, interceptor, baseName, injectionPointToProviderField,
                interceptorToProviderField,
                bindings.getFieldDescriptor());

        implementGetIdentifier(interceptor, interceptorCreator);
        implementCreate(classOutput, interceptorCreator, interceptor, providerTypeName, baseName, injectionPointToProviderField,
                interceptorToProviderField,
                reflectionRegistration, targetPackage, isApplicationClass);
        implementGet(interceptor, interceptorCreator, providerTypeName);
        implementGetTypes(interceptorCreator, beanTypes.getFieldDescriptor());
        implementGetBeanClass(interceptor, interceptorCreator);
        // Interceptors are always @Dependent and have always default qualifiers

        // InjectableInterceptor methods
        implementGetInterceptorBindings(interceptorCreator, bindings.getFieldDescriptor());
        implementIntercepts(interceptorCreator, interceptor);
        implementIntercept(interceptorCreator, interceptor, providerTypeName, reflectionRegistration, isApplicationClass);
        implementGetPriority(interceptorCreator, interceptor);

        interceptorCreator.close();
        return classOutput.getResources();

    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator creator, InterceptorInfo interceptor,
            String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField, FieldDescriptor bindings) {

        MethodCreator constructor = initConstructor(classOutput, creator, interceptor, baseName, injectionPointToProviderField,
                interceptorToProviderField,
                annotationLiterals);

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
    protected void implementIntercept(ClassCreator creator, InterceptorInfo interceptor, String providerTypeName,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {
        MethodCreator intercept = creator
                .getMethodCreator("intercept", Object.class, InterceptionType.class, Object.class, InvocationContext.class)
                .setModifiers(ACC_PUBLIC).addException(Exception.class);

        addIntercept(intercept, interceptor.getAroundInvoke(), InterceptionType.AROUND_INVOKE, providerTypeName,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getPostConstruct(), InterceptionType.POST_CONSTRUCT, providerTypeName,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getPreDestroy(), InterceptionType.PRE_DESTROY, providerTypeName,
                reflectionRegistration, isApplicationClass);
        addIntercept(intercept, interceptor.getAroundConstruct(), InterceptionType.AROUND_CONSTRUCT, providerTypeName,
                reflectionRegistration, isApplicationClass);
        intercept.returnValue(intercept.loadNull());
    }

    private void addIntercept(MethodCreator intercept, MethodInfo interceptorMethod, InterceptionType interceptionType,
            String providerTypeName,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass) {
        if (interceptorMethod != null) {
            ResultHandle enumValue = intercept
                    .readStaticField(FieldDescriptor.of(InterceptionType.class.getName(), interceptionType.name(),
                            InterceptionType.class.getName()));
            BranchResult result = intercept.ifNonZero(
                    intercept.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, enumValue, intercept.getMethodParam(0)));
            BytecodeCreator trueBranch = result.trueBranch();
            Class<?> retType = InterceptionType.AROUND_INVOKE.equals(interceptionType) ? Object.class : void.class;
            ResultHandle ret;
            if (Modifier.isPrivate(interceptorMethod.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Interceptor method %s#%s()", interceptorMethod.declaringClass().name(),
                                interceptorMethod.name()));
                // Use reflection fallback
                ResultHandle paramTypesArray = trueBranch.newArray(Class.class, trueBranch.load(1));
                trueBranch.writeArrayValue(paramTypesArray, 0, trueBranch.loadClass(InvocationContext.class));
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
                        MethodDescriptor.ofMethod(providerTypeName, interceptorMethod.name(), retType, InvocationContext.class),
                        intercept.getMethodParam(1), intercept.getMethodParam(2));
            }
            trueBranch.returnValue(InterceptionType.AROUND_INVOKE.equals(interceptionType) ? ret : trueBranch.loadNull());
        }
    }
}
