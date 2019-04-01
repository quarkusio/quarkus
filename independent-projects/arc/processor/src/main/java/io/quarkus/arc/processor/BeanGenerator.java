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

import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.CreationalContextImpl;
import io.quarkus.arc.CurrentInjectionPointProvider;
import io.quarkus.arc.InitializedInterceptor;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.LazyValue;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import java.lang.reflect.Member;
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.literal.InjectLiteral;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class BeanGenerator extends AbstractGenerator {

    static final String BEAN_SUFFIX = "_Bean";

    static final String PRODUCER_METHOD_SUFFIX = "_ProducerMethod";

    static final String PRODUCER_FIELD_SUFFIX = "_ProducerField";

    static final String SYNTHETIC_SUFFIX = "_Synthetic";

    protected static final String FIELD_NAME_DECLARING_PROVIDER = "declaringProvider";
    protected static final String FIELD_NAME_BEAN_TYPES = "types";
    protected static final String FIELD_NAME_QUALIFIERS = "qualifiers";
    protected static final String FIELD_NAME_STEREOTYPES = "stereotypes";
    protected static final String FIELD_NAME_PROXY = "proxy";
    protected static final String FIELD_NAME_PARAMS = "params";

    protected final AnnotationLiteralProcessor annotationLiterals;

    protected final Predicate<DotName> applicationClassPredicate;

    protected final PrivateMembersCollector privateMembers;

    public BeanGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers) {
        this.annotationLiterals = annotationLiterals;
        this.applicationClassPredicate = applicationClassPredicate;
        this.privateMembers = privateMembers;
    }

    /**
     *
     * @param bean
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean, ReflectionRegistration reflectionRegistration) {
        if (bean.getTarget().isPresent()) {
            AnnotationTarget target = bean.getTarget().get();
            switch (target.kind()) {
                case CLASS:
                    return generateClassBean(bean, target.asClass(), reflectionRegistration);
                case METHOD:
                    return generateProducerMethodBean(bean, target.asMethod(), reflectionRegistration);
                case FIELD:
                    return generateProducerFieldBean(bean, target.asField(), reflectionRegistration);
                default:
                    throw new IllegalArgumentException("Unsupported bean type");
            }
        } else {
            // Synthetic beans
            return generateSyntheticBean(bean, reflectionRegistration);
        }
    }

    Collection<Resource> generateSyntheticBean(BeanInfo bean, ReflectionRegistration reflectionRegistration) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz());
        }
        baseName += SYNTHETIC_SUFFIX;

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String targetPackage = getPackageName(bean);
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(bean.getImplClazz().name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        // If needed, store the synthetic bean parameters
        FieldCreator params = beanCreator.getFieldCreator(FIELD_NAME_PARAMS, Map.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        MethodCreator constructor = initConstructor(classOutput, beanCreator, bean, baseName, Collections.emptyMap(),
                Collections.emptyMap(),
                annotationLiterals);
        ResultHandle paramsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
        for (Entry<String, Object> entry : bean.getParams().entrySet()) {
            ResultHandle valHandle = null;
            if (entry.getValue() instanceof String) {
                valHandle = constructor.load(entry.getValue().toString());
            } else if (entry.getValue() instanceof Integer) {
                valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                        constructor.load(((Integer) entry.getValue()).intValue()));
            } else if (entry.getValue() instanceof Long) {
                valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Long.class, long.class),
                        constructor.load(((Long) entry.getValue()).longValue()));
            } else if (entry.getValue() instanceof Double) {
                valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Double.class, double.class),
                        constructor.load(((Double) entry.getValue()).doubleValue()));
            } else if (entry.getValue() instanceof Class) {
                valHandle = constructor.loadClass((Class<?>) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                valHandle = constructor.load((Boolean) entry.getValue());
            }
            // TODO other param types
            constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, paramsHandle, constructor.load(entry.getKey()),
                    valHandle);
        }
        constructor.writeInstanceField(params.getFieldDescriptor(), constructor.getThis(), paramsHandle);
        constructor.returnValue(null);

        implementGetIdentifier(bean, beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, Collections.emptyMap(), reflectionRegistration,
                    isApplicationClass);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, Collections.emptyMap(),
                Collections.emptyMap(), reflectionRegistration,
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        if (bean.isAlternative()) {
            implementGetAlternativePriority(bean, beanCreator);
        }
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateClassBean(BeanInfo bean, ClassInfo beanClass, ReflectionRegistration reflectionRegistration) {

        String baseName;
        if (beanClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(beanClass.enclosingClass()) + "_" + DotNames.simpleName(beanClass);
        } else {
            baseName = DotNames.simpleName(beanClass);
        }
        Type providerType = bean.getProviderType();
        ClassInfo providerClass = bean.getDeployment().getIndex().getClassByName(providerType.name());
        String providerTypeName = providerClass.name().toString();
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(beanClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToProviderField = new HashMap<>();
        initMaps(bean, injectionPointToProviderField, interceptorToProviderField);

        createProviderFields(beanCreator, bean, injectionPointToProviderField, interceptorToProviderField);
        createConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, interceptorToProviderField,
                annotationLiterals);

        implementGetIdentifier(bean, beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderField, reflectionRegistration,
                    isApplicationClass);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, injectionPointToProviderField,
                interceptorToProviderField,
                reflectionRegistration, targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        if (bean.isAlternative()) {
            implementGetAlternativePriority(bean, beanCreator);
        }
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerMethodBean(BeanInfo bean, MethodInfo producerMethod,
            ReflectionRegistration reflectionRegistration) {

        ClassInfo declaringClass = producerMethod.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + "_"
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        Type providerType = bean.getProviderType();
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(producerMethod.name())
                .append("_")
                .append(producerMethod.returnType().name().toString());

        for (Type i : producerMethod.parameters()) {
            sigBuilder.append(i.name().toString());
        }

        String baseName = declaringClassBase + PRODUCER_METHOD_SUFFIX + "_" + producerMethod.name() + "_"
                + Hashes.sha1(sigBuilder.toString());
        String providerTypeName = providerType.name().toString();
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        // Producer methods are not intercepted
        initMaps(bean, injectionPointToProviderField, null);

        createProviderFields(beanCreator, bean, injectionPointToProviderField, Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, Collections.emptyMap(),
                annotationLiterals);

        implementGetIdentifier(bean, beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderField, reflectionRegistration,
                    isApplicationClass);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, injectionPointToProviderField,
                Collections.emptyMap(),
                reflectionRegistration, targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerFieldBean(BeanInfo bean, FieldInfo producerField,
            ReflectionRegistration reflectionRegistration) {

        ClassInfo declaringClass = producerField.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + "_"
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        Type providerType = bean.getProviderType();
        String baseName = declaringClassBase + PRODUCER_FIELD_SUFFIX + "_" + producerField.name();
        String providerTypeName = providerType.name().toString();
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, LazyValue.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        createProviderFields(beanCreator, bean, Collections.emptyMap(), Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, baseName, Collections.emptyMap(), Collections.emptyMap(),
                annotationLiterals);

        implementGetIdentifier(bean, beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, null, reflectionRegistration, isApplicationClass);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, Collections.emptyMap(),
                Collections.emptyMap(), reflectionRegistration,
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }
        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    protected void initMaps(BeanInfo bean, Map<InjectionPointInfo, String> injectionPointToProvider,
            Map<InterceptorInfo, String> interceptorToProvider) {
        int providerIdx = 1;
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            injectionPointToProvider.put(injectionPoint, "injectProvider" + providerIdx++);
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                injectionPointToProvider.put(injectionPoint, "disposerProvider" + providerIdx++);
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            interceptorToProvider.put(interceptor, "interceptorProvider" + providerIdx++);
        }
    }

    protected void createProviderFields(ClassCreator beanCreator, BeanInfo bean,
            Map<InjectionPointInfo, String> injectionPointToProvider,
            Map<InterceptorInfo, String> interceptorToProvider) {
        // Declaring bean provider
        if (bean.isProducerMethod() || bean.isProducerField()) {
            beanCreator.getFieldCreator(FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
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
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            AnnotationLiteralProcessor annotationLiterals) {
        initConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, interceptorToProviderField,
                annotationLiterals)
                        .returnValue(null);
    }

    protected MethodCreator initConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
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

        MethodCreator constructor = beanCreator.getMethodCreator(Methods.INIT, "V", parameterTypes.toArray(new String[0]));
        // Invoke super()
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());

        // Declaring bean provider
        int paramIdx = 0;
        if (bean.isProducerMethod() || bean.isProducerField()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER,
                            InjectableBean.class.getName()),
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
                        .anyMatch(ip -> BuiltinBean.INJECTION_POINT.getRawTypeDotName().equals(ip.getRequiredType().name()))) {
                    // Injection point resolves to a dependent bean that injects InjectionPoint metadata and so we need to wrap the injectable
                    // reference provider
                    ResultHandle wrapHandle = wrapCurrentInjectionPoint(classOutput, beanCreator, bean, constructor,
                            injectionPoint, paramIdx++);
                    constructor.writeInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                                    InjectableReferenceProvider.class.getName()),
                            constructor.getThis(), wrapHandle);
                } else {
                    constructor.writeInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                                    InjectableReferenceProvider.class.getName()),
                            constructor.getThis(), constructor.getMethodParam(paramIdx++));
                }
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                            InjectableInterceptor.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(paramIdx++));
        }

        // Bean types
        ResultHandle typesHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (org.jboss.jandex.Type type : bean.getTypes()) {
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, typesHandle, Types.getTypeHandle(constructor, type));
        }
        ResultHandle unmodifiableTypesHandle = constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_UNMODIFIABLE_SET,
                typesHandle);
        constructor.writeInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_BEAN_TYPES, Set.class.getName()),
                constructor.getThis(),
                unmodifiableTypesHandle);

        // Qualifiers
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {

            ResultHandle qualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));

            for (AnnotationInstance qualifierAnnotation : bean.getQualifiers()) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            qualifier.getLiteralInstance(constructor));
                } else {
                    // Create annotation literal first
                    ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifierAnnotation.name());
                    constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                            annotationLiterals.process(constructor, classOutput,
                                    qualifierClass, qualifierAnnotation, Types.getPackageName(beanCreator.getClassName())));
                }
            }
            ResultHandle unmodifiableQualifiersHandle = constructor
                    .invokeStaticMethod(MethodDescriptors.COLLECTIONS_UNMODIFIABLE_SET, qualifiersHandle);
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_QUALIFIERS, Set.class.getName()),
                    constructor.getThis(),
                    unmodifiableQualifiersHandle);
        }

        // Stereotypes
        if (!bean.getStereotypes().isEmpty()) {
            ResultHandle stereotypesHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (StereotypeInfo stereotype : bean.getStereotypes()) {
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, stereotypesHandle,
                        constructor.loadClass(stereotype.getTarget().name().toString()));
            }
            ResultHandle unmodifiableStereotypesHandle = constructor
                    .invokeStaticMethod(MethodDescriptors.COLLECTIONS_UNMODIFIABLE_SET, stereotypesHandle);
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_STEREOTYPES, Set.class.getName()),
                    constructor.getThis(),
                    unmodifiableStereotypesHandle);
        }

        if (bean.getScope().isNormal()) {
            // this.proxy = new LazyValue(() -> new Bar_ClientProxy(this))
            String proxyTypeName = getPackageName(bean) + "." + baseName + ClientProxyGenerator.CLIENT_PROXY_SUFFIX;
            FunctionCreator func = constructor.createFunction(Supplier.class);
            BytecodeCreator funcBytecode = func.getBytecode();
            funcBytecode
                    .returnValue(funcBytecode.newInstance(
                            MethodDescriptor.ofConstructor(proxyTypeName, beanCreator.getClassName()), constructor.getThis()));

            ResultHandle proxyHandle = constructor.newInstance(MethodDescriptor.ofConstructor(LazyValue.class, Supplier.class),
                    func.getInstance());
            constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), "proxy", LazyValue.class.getName()),
                    constructor.getThis(),
                    proxyHandle);
        }

        return constructor;
    }

    protected void implementDestroy(BeanInfo bean, ClassCreator beanCreator, String providerTypeName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, ReflectionRegistration reflectionRegistration,
            boolean isApplicationClass) {

        MethodCreator destroy = beanCreator.getMethodCreator("destroy", void.class, providerTypeName, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (bean.isClassBean()) {
            if (!bean.isInterceptor()) {
                // PreDestroy interceptors
                if (!bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()) {
                    destroy.invokeInterfaceMethod(MethodDescriptor.ofMethod(Subclass.class, "destroy", void.class),
                            destroy.getMethodParam(0));
                }

                // PreDestroy callbacks
                List<MethodInfo> preDestroyCallbacks = Beans.getCallbacks(bean.getTarget().get().asClass(),
                        DotNames.PRE_DESTROY,
                        bean.getDeployment().getIndex());
                for (MethodInfo callback : preDestroyCallbacks) {
                    if (Modifier.isPrivate(callback.flags())) {
                        privateMembers.add(isApplicationClass, String.format("@PreDestroy callback %s#%s()",
                                callback.declaringClass().name(), callback.name()));
                        reflectionRegistration.registerMethod(callback);
                        destroy.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                                destroy.loadClass(callback.declaringClass().name().toString()),
                                destroy.load(callback.name()), destroy.newArray(Class.class, destroy.load(0)),
                                destroy.getMethodParam(0),
                                destroy.newArray(Object.class, destroy.load(0)));
                    } else {
                        // instance.superCoolDestroyCallback()
                        destroy.invokeVirtualMethod(MethodDescriptor.of(callback), destroy.getMethodParam(0));
                    }
                }
            }

            // ctx.release()
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, destroy.getMethodParam(1));
            destroy.returnValue(null);

        } else if (bean.getDisposer() != null) {
            // Invoke the disposer method
            // declaringProvider.get(new CreationalContextImpl<>()).dispose()
            MethodInfo disposerMethod = bean.getDisposer().getDisposerMethod();

            ResultHandle declaringProviderHandle = destroy.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER,
                            InjectableBean.class.getName()),
                    destroy.getThis());
            ResultHandle ctxHandle = destroy.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(
                        MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
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
                            injectionPointToProviderField.get(injectionPointsIterator.next()),
                            InjectableReferenceProvider.class.getName()), destroy.getThis());
                    ResultHandle referenceHandle = destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                            providerHandle, childCtxHandle);
                    referenceHandles[i] = referenceHandle;
                }
            }

            if (Modifier.isPrivate(disposerMethod.flags())) {
                privateMembers.add(isApplicationClass, String.format("Disposer %s#%s", disposerMethod.declaringClass().name(),
                        disposerMethod.name()));
                ResultHandle paramTypesArray = destroy.newArray(Class.class, destroy.load(referenceHandles.length));
                ResultHandle argsArray = destroy.newArray(Object.class, destroy.load(referenceHandles.length));
                for (int i = 0; i < referenceHandles.length; i++) {
                    destroy.writeArrayValue(paramTypesArray, i,
                            destroy.loadClass(disposerMethod.parameters().get(i).name().toString()));
                    destroy.writeArrayValue(argsArray, i, referenceHandles[i]);
                }
                reflectionRegistration.registerMethod(disposerMethod);
                destroy.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        destroy.loadClass(disposerMethod.declaringClass().name().toString()),
                        destroy.load(disposerMethod.name()), paramTypesArray, declaringProviderInstanceHandle, argsArray);
            } else {
                destroy.invokeVirtualMethod(MethodDescriptor.of(disposerMethod), declaringProviderInstanceHandle,
                        referenceHandles);
            }

            // Destroy @Dependent instances injected into method parameters of a disposer method
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, ctxHandle);

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getDisposer().getDeclaringBean().getScope())) {
                destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                        declaringProviderInstanceHandle, ctxHandle);
            }
            // ctx.release()
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, destroy.getMethodParam(1));
            destroy.returnValue(null);

        } else if (bean.isSynthetic()) {
            bean.getDestroyerConsumer().accept(destroy);
        }

        // Bridge method needed
        MethodCreator bridgeDestroy = beanCreator.getMethodCreator("destroy", void.class, Object.class, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);
        bridgeDestroy.returnValue(bridgeDestroy.invokeVirtualMethod(destroy.getMethodDescriptor(), bridgeDestroy.getThis(),
                bridgeDestroy.getMethodParam(0),
                bridgeDestroy.getMethodParam(1)));
    }

    protected void implementCreate(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String providerTypeName,
            String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            ReflectionRegistration reflectionRegistration, String targetPackage, boolean isApplicationClass) {

        MethodCreator create = beanCreator.getMethodCreator("create", providerTypeName, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);
        AssignableResultHandle instanceHandle;

        if (bean.isClassBean()) {
            List<Injection> methodInjections = new ArrayList<>();
            List<Injection> fieldInjections = new ArrayList<>();
            for (Injection injection : bean.getInjections()) {
                if (injection.isField()) {
                    fieldInjections.add(injection);
                } else if (injection.isMethod() && !injection.isConstructor()) {
                    methodInjections.add(injection);
                }
            }

            ResultHandle postConstructsHandle = null;
            ResultHandle aroundConstructsHandle = null;
            Map<InterceptorInfo, ResultHandle> interceptorToWrap = new HashMap<>();

            if (bean.hasLifecycleInterceptors()) {
                // Note that we must share the interceptors instances with the intercepted subclass, if present
                InterceptionInfo postConstructs = bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT);
                InterceptionInfo aroundConstructs = bean.getLifecycleInterceptors(InterceptionType.AROUND_CONSTRUCT);

                // Wrap InjectableInterceptors using InitializedInterceptor
                Set<InterceptorInfo> wraps = new HashSet<>();
                wraps.addAll(aroundConstructs.interceptors);
                wraps.addAll(postConstructs.interceptors);
                for (InterceptorInfo interceptor : wraps) {
                    ResultHandle interceptorProvider = create.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                                    InjectableInterceptor.class.getName()),
                            create.getThis());
                    ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(
                            MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorProvider,
                            create.getMethodParam(0));
                    ResultHandle wrapHandle = create.invokeStaticMethod(
                            MethodDescriptor.ofMethod(InitializedInterceptor.class, "of",
                                    InitializedInterceptor.class, Object.class, InjectableInterceptor.class),
                            interceptorInstanceHandle, interceptorProvider);
                    interceptorToWrap.put(interceptor, wrapHandle);
                }

                if (!postConstructs.isEmpty()) {
                    // postConstructs = new ArrayList<InterceptorInvocation>()
                    postConstructsHandle = create.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : postConstructs.interceptors) {
                        ResultHandle interceptorHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()),
                                create.getThis());
                        ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                                create.getMethodParam(0));
                        ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(
                                MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorHandle,
                                childCtxHandle);

                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_POST_CONSTRUCT,
                                interceptorHandle, interceptorInstanceHandle);

                        // postConstructs.add(InterceptorInvocation.postConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                        create.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, postConstructsHandle,
                                interceptorInvocationHandle);
                    }
                }
                if (!aroundConstructs.isEmpty()) {
                    // aroundConstructs = new ArrayList<InterceptorInvocation>()
                    aroundConstructsHandle = create.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : aroundConstructs.interceptors) {
                        ResultHandle interceptorHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                interceptorToProviderField.get(interceptor), InjectableInterceptor.class.getName()),
                                create.getThis());
                        ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                                create.getMethodParam(0));
                        ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(
                                MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorHandle,
                                childCtxHandle);

                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT,
                                interceptorHandle, interceptorInstanceHandle);

                        // aroundConstructs.add(InterceptorInvocation.aroundConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                        create.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, aroundConstructsHandle,
                                interceptorInvocationHandle);
                    }
                }
            }

            // AroundConstruct lifecycle callback interceptors
            InterceptionInfo aroundConstructs = bean.getLifecycleInterceptors(InterceptionType.AROUND_CONSTRUCT);
            instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerTypeName));
            if (!aroundConstructs.isEmpty()) {
                Optional<Injection> constructorInjection = bean.getConstructorInjection();
                ResultHandle constructorHandle;
                if (constructorInjection.isPresent()) {
                    List<String> paramTypes = new ArrayList<>();
                    for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                        paramTypes.add(injectionPoint.getRequiredType().name().toString());
                    }
                    ResultHandle[] paramsHandles = new ResultHandle[2];
                    paramsHandles[0] = create.loadClass(providerTypeName);
                    ResultHandle paramsArray = create.newArray(Class.class, create.load(paramTypes.size()));
                    for (ListIterator<String> iterator = paramTypes.listIterator(); iterator.hasNext();) {
                        create.writeArrayValue(paramsArray, iterator.nextIndex(), create.loadClass(iterator.next()));
                    }
                    paramsHandles[1] = paramsArray;
                    constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                            paramsHandles);
                } else {
                    // constructor = Reflections.findConstructor(Foo.class)
                    ResultHandle[] paramsHandles = new ResultHandle[2];
                    paramsHandles[0] = create.loadClass(providerTypeName);
                    paramsHandles[1] = create.newArray(Class.class, create.load(0));
                    constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                            paramsHandles);
                }

                List<ResultHandle> providerHandles = newProviderHandles(bean, beanCreator, create,
                        injectionPointToProviderField, interceptorToProviderField,
                        interceptorToWrap);

                // Forwarding function
                // Supplier<Object> forward = () -> new SimpleBean_Subclass(ctx,lifecycleInterceptorProvider1)
                FunctionCreator func = create.createFunction(Supplier.class);
                BytecodeCreator funcBytecode = func.getBytecode();
                ResultHandle retHandle = newInstanceHandle(bean, beanCreator, funcBytecode, create, providerTypeName, baseName,
                        providerHandles,
                        reflectionRegistration, isApplicationClass);
                funcBytecode.returnValue(retHandle);
                // Interceptor bindings
                ResultHandle bindingsHandle = create.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
                for (AnnotationInstance binding : aroundConstructs.bindings) {
                    // Create annotation literals first
                    ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                    create.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                            annotationLiterals.process(create, classOutput, bindingClass, binding,
                                    Types.getPackageName(beanCreator.getClassName())));
                }

                // InvocationContextImpl.aroundConstruct(constructor,aroundConstructs,forward).proceed()
                ResultHandle invocationContextHandle = create.invokeStaticMethod(
                        MethodDescriptors.INVOCATION_CONTEXT_AROUND_CONSTRUCT, constructorHandle,
                        aroundConstructsHandle, func.getInstance(), bindingsHandle);
                TryBlock tryCatch = create.tryBlock();
                CatchBlockCreator exceptionCatch = tryCatch.addCatch(Exception.class);
                // throw new RuntimeException(e)
                exceptionCatch.throwException(RuntimeException.class, "Error invoking aroundConstructs",
                        exceptionCatch.getCaughtException());
                tryCatch.assign(instanceHandle,
                        tryCatch.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(InvocationContext.class, "proceed", Object.class),
                                invocationContextHandle));

            } else {
                create.assign(instanceHandle, newInstanceHandle(bean, beanCreator, create, create, providerTypeName, baseName,
                        newProviderHandles(bean, beanCreator, create, injectionPointToProviderField, interceptorToProviderField,
                                interceptorToWrap),
                        reflectionRegistration, isApplicationClass));
            }

            // Perform field and initializer injections
            for (Injection fieldInjection : fieldInjections) {
                InjectionPointInfo injectionPoint = fieldInjection.injectionPoints.get(0);
                ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                        create.getMethodParam(0));
                ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()),
                        create.getThis());
                ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle);

                FieldInfo injectedField = fieldInjection.target.asField();
                if (isReflectionFallbackNeeded(injectedField, targetPackage)) {
                    if (Modifier.isPrivate(injectedField.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@Inject field %s#%s", fieldInjection.target.asField().declaringClass().name(),
                                        fieldInjection.target.asField().name()));
                    }
                    reflectionRegistration.registerField(injectedField);
                    create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_WRITE_FIELD,
                            create.loadClass(injectedField.declaringClass().name().toString()),
                            create.load(injectedField.name()), instanceHandle, referenceHandle);

                } else {
                    create.writeInstanceField(
                            FieldDescriptor.of(providerTypeName, injectedField.name(),
                                    injectionPoint.getRequiredType().name().toString()),
                            instanceHandle, referenceHandle);
                }
            }
            for (Injection methodInjection : methodInjections) {
                ResultHandle[] referenceHandles = new ResultHandle[methodInjection.injectionPoints.size()];
                int paramIdx = 0;
                for (InjectionPointInfo injectionPoint : methodInjection.injectionPoints) {
                    ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                            create.getMethodParam(0));
                    ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                            injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()),
                            create.getThis());
                    ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                            providerHandle, childCtxHandle);
                    referenceHandles[paramIdx++] = referenceHandle;
                }

                MethodInfo initializerMethod = methodInjection.target.asMethod();
                if (isReflectionFallbackNeeded(initializerMethod, targetPackage)) {
                    if (Modifier.isPrivate(initializerMethod.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@Inject initializer %s#%s()", initializerMethod.declaringClass().name(),
                                        initializerMethod.name()));
                    }
                    ResultHandle paramTypesArray = create.newArray(Class.class, create.load(referenceHandles.length));
                    ResultHandle argsArray = create.newArray(Object.class, create.load(referenceHandles.length));
                    for (int i = 0; i < referenceHandles.length; i++) {
                        create.writeArrayValue(paramTypesArray, i,
                                create.loadClass(initializerMethod.parameters().get(i).name().toString()));
                        create.writeArrayValue(argsArray, i, referenceHandles[i]);
                    }
                    reflectionRegistration.registerMethod(initializerMethod);
                    create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                            create.loadClass(initializerMethod.declaringClass().name().toString()),
                            create.load(methodInjection.target.asMethod().name()),
                            paramTypesArray, instanceHandle, argsArray);

                } else {
                    create.invokeVirtualMethod(MethodDescriptor.of(methodInjection.target.asMethod()), instanceHandle,
                            referenceHandles);
                }
            }

            // PostConstruct lifecycle callback interceptors
            InterceptionInfo postConstructs = bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT);
            if (!postConstructs.isEmpty()) {

                // Interceptor bindings
                ResultHandle bindingsHandle = create.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
                for (AnnotationInstance binding : postConstructs.bindings) {
                    // Create annotation literals first
                    ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                    create.invokeInterfaceMethod(MethodDescriptors.SET_ADD, bindingsHandle,
                            annotationLiterals.process(create, classOutput, bindingClass, binding,
                                    Types.getPackageName(beanCreator.getClassName())));
                }

                // InvocationContextImpl.postConstruct(instance,postConstructs).proceed()
                ResultHandle invocationContextHandle = create.invokeStaticMethod(
                        MethodDescriptors.INVOCATION_CONTEXT_POST_CONSTRUCT, instanceHandle,
                        postConstructsHandle, bindingsHandle);

                TryBlock tryCatch = create.tryBlock();
                CatchBlockCreator exceptionCatch = tryCatch.addCatch(Exception.class);
                // throw new RuntimeException(e)
                exceptionCatch.throwException(RuntimeException.class, "Error invoking postConstructs",
                        exceptionCatch.getCaughtException());
                tryCatch.invokeInterfaceMethod(MethodDescriptor.ofMethod(InvocationContext.class, "proceed", Object.class),
                        invocationContextHandle);
            }

            // PostConstruct callbacks
            if (!bean.isInterceptor()) {
                List<MethodInfo> postConstructCallbacks = Beans.getCallbacks(bean.getTarget().get().asClass(),
                        DotNames.POST_CONSTRUCT,
                        bean.getDeployment().getIndex());
                for (MethodInfo callback : postConstructCallbacks) {
                    if (Modifier.isPrivate(callback.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@PostConstruct callback %s#%s()", callback.declaringClass().name(),
                                        callback.name()));
                        reflectionRegistration.registerMethod(callback);
                        create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                                create.loadClass(callback.declaringClass().name().toString()),
                                create.load(callback.name()), create.newArray(Class.class, create.load(0)), instanceHandle,
                                create.newArray(Object.class, create.load(0)));
                    } else {
                        create.invokeVirtualMethod(MethodDescriptor.of(callback), instanceHandle);
                    }
                }
            }
            create.returnValue(instanceHandle);

        } else if (bean.isProducerMethod()) {
            instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerTypeName));
            // instance = declaringProvider.get(new CreationalContextImpl<>()).produce()
            ResultHandle declaringProviderHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER,
                            InjectableBean.class.getName()),
                    create.getThis());
            ResultHandle ctxHandle = create.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                        MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle);
            }

            List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
            ResultHandle[] referenceHandles = new ResultHandle[injectionPoints.size()];
            int paramIdx = 0;
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                        create.getMethodParam(0));
                ResultHandle providerHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()),
                        create.getThis());
                ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle);
                referenceHandles[paramIdx++] = referenceHandle;
            }

            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            if (Modifier.isPrivate(producerMethod.flags())) {
                privateMembers.add(isApplicationClass, String.format("Producer method %s#%s()",
                        producerMethod.declaringClass().name(), producerMethod.name()));
                ResultHandle paramTypesArray = create.newArray(Class.class, create.load(referenceHandles.length));
                ResultHandle argsArray = create.newArray(Object.class, create.load(referenceHandles.length));
                for (int i = 0; i < referenceHandles.length; i++) {
                    create.writeArrayValue(paramTypesArray, i,
                            create.loadClass(producerMethod.parameters().get(i).name().toString()));
                    create.writeArrayValue(argsArray, i, referenceHandles[i]);
                }
                reflectionRegistration.registerMethod(producerMethod);
                create.assign(instanceHandle, create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        create.loadClass(producerMethod.declaringClass().name().toString()), create.load(producerMethod.name()),
                        paramTypesArray,
                        declaringProviderInstanceHandle, argsArray));
            } else {
                create.assign(instanceHandle, create.invokeVirtualMethod(MethodDescriptor.of(producerMethod),
                        declaringProviderInstanceHandle, referenceHandles));
            }

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope())) {
                create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                        declaringProviderInstanceHandle, ctxHandle);
            }
            create.returnValue(instanceHandle);

        } else if (bean.isProducerField()) {
            instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerTypeName));
            // instance = declaringProvider.get(new CreationalContextImpl<>()).field

            FieldInfo producerField = bean.getTarget().get().asField();

            ResultHandle declaringProviderHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER,
                            InjectableBean.class.getName()),
                    create.getThis());
            ResultHandle ctxHandle = create.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                    MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                    ctxHandle);

            if (bean.getDeclaringBean().getScope().isNormal()) {
                // We need to unwrap the client proxy
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                        MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                        declaringProviderInstanceHandle);
            }

            if (Modifier.isPrivate(producerField.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Producer field %s#%s", producerField.declaringClass().name(), producerField.name()));
                reflectionRegistration.registerField(producerField);
                create.assign(instanceHandle, create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_READ_FIELD,
                        create.loadClass(producerField.declaringClass().name().toString()), create.load(producerField.name()),
                        declaringProviderInstanceHandle));
            } else {
                create.assign(instanceHandle,
                        create.readInstanceField(FieldDescriptor.of(producerField), declaringProviderInstanceHandle));
            }

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope())) {
                create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                        declaringProviderInstanceHandle, ctxHandle);
            }
            create.returnValue(instanceHandle);

        } else if (bean.isSynthetic()) {
            bean.getCreatorConsumer().accept(create);
        }

        // Bridge method needed
        MethodCreator bridgeCreate = beanCreator.getMethodCreator("create", Object.class, CreationalContext.class)
                .setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeCreate.returnValue(bridgeCreate.invokeVirtualMethod(create.getMethodDescriptor(), bridgeCreate.getThis(),
                bridgeCreate.getMethodParam(0)));
    }

    private List<ResultHandle> newProviderHandles(BeanInfo bean, ClassCreator beanCreator, MethodCreator createMethod,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            Map<InterceptorInfo, ResultHandle> interceptorToWrap) {

        List<ResultHandle> providerHandles = new ArrayList<>();
        Optional<Injection> constructorInjection = bean.getConstructorInjection();

        if (constructorInjection.isPresent()) {
            for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                ResultHandle providerHandle = createMethod.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderField.get(injectionPoint), InjectableReferenceProvider.class.getName()),
                        createMethod.getThis());
                ResultHandle childCtx = createMethod.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                        createMethod.getMethodParam(0));
                providerHandles.add(createMethod.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtx));
            }
        }
        if (bean.isSubclassRequired()) {
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                ResultHandle wrapped = interceptorToWrap.get(interceptor);
                if (wrapped != null) {
                    providerHandles.add(wrapped);
                } else {
                    providerHandles.add(createMethod.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                                    InjectableInterceptor.class.getName()),
                            createMethod.getThis()));
                }
            }
        }
        return providerHandles;
    }

    private ResultHandle newInstanceHandle(BeanInfo bean, ClassCreator beanCreator, BytecodeCreator creator,
            MethodCreator createMethod,
            String providerTypeName, String baseName, List<ResultHandle> providerHandles, ReflectionRegistration registration,
            boolean isApplicationClass) {

        Optional<Injection> constructorInjection = bean.getConstructorInjection();
        MethodInfo constructor = constructorInjection.isPresent() ? constructorInjection.get().target.asMethod() : null;
        List<InjectionPointInfo> injectionPoints = constructorInjection.isPresent() ? constructorInjection.get().injectionPoints
                : Collections.emptyList();

        if (bean.isSubclassRequired()) {
            // new SimpleBean_Subclass(foo,ctx,lifecycleInterceptorProvider1)

            List<InterceptorInfo> interceptors = bean.getBoundInterceptors();
            List<String> paramTypes = new ArrayList<>();
            List<ResultHandle> paramHandles = new ArrayList<>();

            // 1. constructor injection points
            for (int i = 0; i < injectionPoints.size(); i++) {
                paramTypes.add(injectionPoints.get(i).getRequiredType().name().toString());
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
                    MethodDescriptor.ofConstructor(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName),
                            paramTypes.toArray(new String[0])),
                    paramHandles.toArray(new ResultHandle[0]));

        } else if (constructorInjection.isPresent()) {
            if (Modifier.isPrivate(constructor.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Bean constructor %s on %s", constructor, constructor.declaringClass().name()));
                ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(providerHandles.size()));
                ResultHandle argsArray = creator.newArray(Object.class, creator.load(providerHandles.size()));
                for (int i = 0; i < injectionPoints.size(); i++) {
                    creator.writeArrayValue(paramTypesArray, i,
                            creator.loadClass(injectionPoints.get(i).getRequiredType().name().toString()));
                    creator.writeArrayValue(argsArray, i, providerHandles.get(i));
                }
                registration.registerMethod(constructor);
                return creator.invokeStaticMethod(MethodDescriptors.REFLECTIONS_NEW_INSTANCE,
                        creator.loadClass(constructor.declaringClass().name().toString()),
                        paramTypesArray, argsArray);
            } else {
                // new SimpleBean(foo)
                String[] paramTypes = new String[injectionPoints.size()];
                for (ListIterator<InjectionPointInfo> iterator = injectionPoints.listIterator(); iterator.hasNext();) {
                    InjectionPointInfo injectionPoint = iterator.next();
                    paramTypes[iterator.previousIndex()] = injectionPoint.getRequiredType().name().toString();
                }
                return creator.newInstance(MethodDescriptor.ofConstructor(providerTypeName, paramTypes),
                        providerHandles.toArray(new ResultHandle[0]));
            }
        } else {
            MethodInfo noArgsConstructor = bean.getTarget().get().asClass().method(Methods.INIT);
            if (Modifier.isPrivate(noArgsConstructor.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Bean constructor %s on %s", noArgsConstructor,
                                noArgsConstructor.declaringClass().name()));
                ResultHandle paramTypesArray = creator.newArray(Class.class, creator.load(0));
                ResultHandle argsArray = creator.newArray(Object.class, creator.load(0));

                registration.registerMethod(noArgsConstructor);
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
     * @see InjectableReferenceProvider#get(CreationalContext)
     */
    protected void implementGet(BeanInfo bean, ClassCreator beanCreator, String providerTypeName) {

        MethodCreator get = beanCreator.getMethodCreator("get", providerTypeName, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            // Foo instance = create(ctx)
            ResultHandle instance = get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), "create", providerTypeName, CreationalContext.class),
                    get.getThis(),
                    get.getMethodParam(0));
            // CreationalContextImpl.addDependencyToParent(this,instance,ctx)
            get.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_ADD_DEP_TO_PARENT, get.getThis(), instance,
                    get.getMethodParam(0));
            // return instance
            get.returnValue(instance);
        } else if (BuiltinScope.SINGLETON.is(bean.getScope())) {
            // return Arc.container().getContext(getScope()).get(this, new CreationalContextImpl<>())
            ResultHandle container = get.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
            ResultHandle creationalContext = get.newInstance(MethodDescriptor.ofConstructor(CreationalContextImpl.class));
            ResultHandle scope = get.loadClass(bean.getScope().getDotName().toString());
            ResultHandle context = get.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT, container,
                    scope);
            get.returnValue(
                    get.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET, context, get.getThis(), creationalContext));
        } else if (bean.getScope().isNormal()) {
            // return proxy.get()
            ResultHandle proxy = get.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_PROXY, LazyValue.class.getName()), get.getThis());
            get.returnValue(get.invokeVirtualMethod(MethodDescriptors.LAZY_VALUE_GET, proxy));
        } else {
            ResultHandle instance = get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), "create", providerTypeName, CreationalContext.class),
                    get.getThis(),
                    get.getMethodParam(0));
            get.returnValue(instance);
        }

        // Bridge method needed
        MethodCreator bridgeGet = beanCreator.getMethodCreator("get", Object.class, CreationalContext.class)
                .setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeGet.returnValue(
                bridgeGet.invokeVirtualMethod(get.getMethodDescriptor(), bridgeGet.getThis(), bridgeGet.getMethodParam(0)));
    }

    /**
     *
     * @param beanCreator
     * @see InjectableBean#getTypes()
     */
    protected void implementGetTypes(ClassCreator beanCreator, FieldDescriptor typesField) {
        MethodCreator getScope = beanCreator.getMethodCreator("getTypes", Set.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope.readInstanceField(typesField, getScope.getThis()));
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @see InjectableBean#getScope()
     */
    protected void implementGetScope(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getScope = beanCreator.getMethodCreator("getScope", Class.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope.loadClass(bean.getScope().getDotName().toString()));
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @see InjectableBean#getIdentifier()
     */
    protected void implementGetIdentifier(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getScope = beanCreator.getMethodCreator("getIdentifier", String.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope.load(bean.getIdentifier()));
    }

    /**
     *
     * @param bean
     * @param beanCreator
     * @param qualifiersField
     * @see InjectableBean#getQualifiers()
     */
    protected void implementGetQualifiers(BeanInfo bean, ClassCreator beanCreator, FieldDescriptor qualifiersField) {
        MethodCreator getQualifiers = beanCreator.getMethodCreator("getQualifiers", Set.class).setModifiers(ACC_PUBLIC);
        getQualifiers.returnValue(getQualifiers.readInstanceField(qualifiersField, getQualifiers.getThis()));
    }

    protected void implementGetDeclaringBean(ClassCreator beanCreator) {
        MethodCreator getDeclaringBean = beanCreator.getMethodCreator("getDeclaringBean", InjectableBean.class)
                .setModifiers(ACC_PUBLIC);
        getDeclaringBean.returnValue(getDeclaringBean.readInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER, InjectableBean.class.getName()),
                getDeclaringBean.getThis()));
    }

    protected void implementGetAlternativePriority(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getAlternativePriority = beanCreator.getMethodCreator("getAlternativePriority", Integer.class)
                .setModifiers(ACC_PUBLIC);
        getAlternativePriority
                .returnValue(getAlternativePriority.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                        getAlternativePriority.load(bean.getAlternativePriority())));
    }

    protected void implementGetStereotypes(BeanInfo bean, ClassCreator beanCreator, FieldDescriptor stereotypesField) {
        MethodCreator getStereotypes = beanCreator.getMethodCreator("getStereotypes", Set.class).setModifiers(ACC_PUBLIC);
        getStereotypes.returnValue(getStereotypes.readInstanceField(stereotypesField, getStereotypes.getThis()));
    }

    protected void implementGetBeanClass(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getBeanClass = beanCreator.getMethodCreator("getBeanClass", Class.class).setModifiers(ACC_PUBLIC);
        getBeanClass.returnValue(getBeanClass.loadClass(bean.getBeanClass().toString()));
    }

    protected void implementGetName(BeanInfo bean, ClassCreator beanCreator) {
        if (bean.getName() != null) {
            MethodCreator getName = beanCreator.getMethodCreator("getName", String.class)
                    .setModifiers(ACC_PUBLIC);
            getName.returnValue(getName.load(bean.getName()));
        }
    }

    private ResultHandle wrapCurrentInjectionPoint(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            MethodCreator constructor, InjectionPointInfo injectionPoint, int paramIdx) {
        ResultHandle requiredQualifiersHandle = collectQualifiers(classOutput, beanCreator, bean.getDeployment(), constructor,
                injectionPoint,
                annotationLiterals);
        ResultHandle annotationsHandle = collectAnnotations(classOutput, beanCreator, bean.getDeployment(), constructor,
                injectionPoint, annotationLiterals);
        ResultHandle javaMemberHandle = getJavaMemberHandle(constructor, injectionPoint);
        return constructor.newInstance(
                MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableBean.class,
                        InjectableReferenceProvider.class, java.lang.reflect.Type.class,
                        Set.class, Set.class, Member.class, int.class),
                constructor.getThis(), constructor.getMethodParam(paramIdx),
                Types.getTypeHandle(constructor, injectionPoint.getRequiredType()),
                requiredQualifiersHandle, annotationsHandle, javaMemberHandle, constructor.load(injectionPoint.getPosition()));
    }

    static ResultHandle getJavaMemberHandle(MethodCreator constructor,
            InjectionPointInfo injectionPoint) {
        ResultHandle javaMemberHandle;
        if (Kind.FIELD.equals(injectionPoint.getTarget().kind())) {
            FieldInfo field = injectionPoint.getTarget().asField();
            javaMemberHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_FIELD,
                    constructor.loadClass(field.declaringClass().name().toString()),
                    constructor.load(field.name()));
        } else {
            MethodInfo method = injectionPoint.getTarget().asMethod();
            if (method.name().equals(Methods.INIT)) {
                // Reflections.findConstructor(org.foo.SimpleBean.class,java.lang.String.class)                
                ResultHandle[] paramsHandles = new ResultHandle[2];
                paramsHandles[0] = constructor.loadClass(method.declaringClass().name().toString());
                ResultHandle paramsArray = constructor.newArray(Class.class, constructor.load(method.parameters().size()));
                for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
                    constructor.writeArrayValue(paramsArray, iterator.nextIndex(),
                            constructor.loadClass(iterator.next().name().toString()));
                }
                paramsHandles[1] = paramsArray;
                javaMemberHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                        paramsHandles);
            } else {
                // Reflections.findMethod(org.foo.SimpleBean.class,"foo",java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[3];
                paramsHandles[0] = constructor.loadClass(method.declaringClass().name().toString());
                paramsHandles[1] = constructor.load(method.name());
                ResultHandle paramsArray = constructor.newArray(Class.class, constructor.load(method.parameters().size()));
                for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
                    constructor.writeArrayValue(paramsArray, iterator.nextIndex(),
                            constructor.loadClass(iterator.next().name().toString()));
                }
                paramsHandles[2] = paramsArray;
                javaMemberHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD, paramsHandles);
            }
        }
        return javaMemberHandle;
    }

    static ResultHandle collectAnnotations(ClassOutput classOutput, ClassCreator beanCreator, BeanDeployment beanDeployment,
            MethodCreator constructor,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals) {
        ResultHandle annotationsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        Collection<AnnotationInstance> annotations;
        if (Kind.FIELD.equals(injectionPoint.getTarget().kind())) {
            FieldInfo field = injectionPoint.getTarget().asField();
            annotations = beanDeployment.getAnnotations(field);
        } else {
            MethodInfo method = injectionPoint.getTarget().asMethod();
            annotations = InjectionPointInfo.getParameterAnnotations(beanDeployment,
                    method, injectionPoint.getPosition());
        }
        for (AnnotationInstance annotation : annotations) {
            if (DotNames.INJECT.equals(annotation.name())) {
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotationsHandle,
                        constructor.readStaticField(FieldDescriptor.of(InjectLiteral.class, "INSTANCE", InjectLiteral.class)));
            } else {
                // Create annotation literal if needed
                ClassInfo literalClass = beanDeployment.getIndex().getClassByName(annotation.name());
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotationsHandle,
                        annotationLiterals.process(constructor,
                                classOutput, literalClass, annotation,
                                Types.getPackageName(beanCreator.getClassName())));
            }
        }
        return annotationsHandle;
    }

    static ResultHandle collectQualifiers(ClassOutput classOutput, ClassCreator beanCreator, BeanDeployment beanDeployment,
            MethodCreator constructor,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals) {
        ResultHandle requiredQualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
            BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
            if (qualifier != null) {
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle,
                        qualifier.getLiteralInstance(constructor));
            } else {
                // Create annotation literal if needed
                ClassInfo qualifierClass = beanDeployment.getQualifier(qualifierAnnotation.name());
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, requiredQualifiersHandle,
                        annotationLiterals.process(constructor,
                                classOutput, qualifierClass, qualifierAnnotation,
                                Types.getPackageName(beanCreator.getClassName())));
            }
        }
        return requiredQualifiersHandle;
    }

}
