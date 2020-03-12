package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.CurrentInjectionPointProvider;
import io.quarkus.arc.impl.InitializedInterceptor;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
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
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.IllegalProductException;
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

    protected static final String FIELD_NAME_DECLARING_PROVIDER_SUPPLIER = "declaringProviderSupplier";
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

        StringBuilder baseNameBuilder = new StringBuilder();
        if (bean.getImplClazz().enclosingClass() != null) {
            baseNameBuilder.append(DotNames.simpleName(bean.getImplClazz().enclosingClass())).append(UNDERSCORE)
                    .append(DotNames.simpleName(bean.getImplClazz()));
        } else {
            baseNameBuilder.append(DotNames.simpleName(bean.getImplClazz()));
        }
        baseNameBuilder.append(UNDERSCORE);
        baseNameBuilder.append(bean.getIdentifier());
        baseNameBuilder.append(UNDERSCORE);
        baseNameBuilder.append(SYNTHETIC_SUFFIX);
        String baseName = baseNameBuilder.toString();

        Type providerType = bean.getProviderType();
        String providerTypeName = providerType.name().toString();
        String targetPackage = getPackageName(bean);
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(bean.getImplClazz().name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class, Supplier.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, getProxyTypeName(bean, baseName))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        MethodCreator constructor = initConstructor(classOutput, beanCreator, bean, baseName, Collections.emptyMap(),
                Collections.emptyMap(),
                annotationLiterals, reflectionRegistration);

        FieldCreator params = beanCreator.getFieldCreator(FIELD_NAME_PARAMS, Map.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        // If needed, store the synthetic bean parameters
        ResultHandle paramsHandle;
        if (bean.getParams().isEmpty()) {
            paramsHandle = constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_EMPTY_MAP);
        } else {
            paramsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
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
        }
        constructor.writeInstanceField(params.getFieldDescriptor(), constructor.getThis(), paramsHandle);
        constructor.returnValue(null);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, Collections.emptyMap(), reflectionRegistration,
                    isApplicationClass, baseName);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, Collections.emptyMap(),
                Collections.emptyMap(), reflectionRegistration,
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName, baseName);

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
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateClassBean(BeanInfo bean, ClassInfo beanClass, ReflectionRegistration reflectionRegistration) {

        String baseName;
        if (beanClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(beanClass.enclosingClass()) + UNDERSCORE + DotNames.simpleName(beanClass);
        } else {
            baseName = DotNames.simpleName(beanClass);
        }
        Type providerType = bean.getProviderType();
        String providerTypeName = providerType.name().toString();
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(beanClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class, Supplier.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, getProxyTypeName(bean, baseName))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderSupplierField = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToProviderSupplierField = new HashMap<>();
        initMaps(bean, injectionPointToProviderSupplierField, interceptorToProviderSupplierField);

        createProviderFields(beanCreator, bean, injectionPointToProviderSupplierField, interceptorToProviderSupplierField);
        createConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderSupplierField,
                interceptorToProviderSupplierField,
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderSupplierField, reflectionRegistration,
                    isApplicationClass, baseName);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, injectionPointToProviderSupplierField,
                interceptorToProviderSupplierField,
                reflectionRegistration, targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName, baseName);

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
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerMethodBean(BeanInfo bean, MethodInfo producerMethod,
            ReflectionRegistration reflectionRegistration) {

        ClassInfo declaringClass = producerMethod.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + UNDERSCORE
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        Type providerType = bean.getProviderType();
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(producerMethod.name())
                .append(UNDERSCORE)
                .append(producerMethod.returnType().name().toString());

        for (Type i : producerMethod.parameters()) {
            sigBuilder.append(i.name().toString());
        }

        String baseName = declaringClassBase + PRODUCER_METHOD_SUFFIX + UNDERSCORE + producerMethod.name() + UNDERSCORE
                + Hashes.sha1(sigBuilder.toString());
        String providerTypeName = providerType.name().toString();
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class, Supplier.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, getProxyTypeName(bean, baseName))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
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
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, injectionPointToProviderField, reflectionRegistration,
                    isApplicationClass, baseName);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, injectionPointToProviderField,
                Collections.emptyMap(),
                reflectionRegistration, targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName, baseName);

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
        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerFieldBean(BeanInfo bean, FieldInfo producerField,
            ReflectionRegistration reflectionRegistration) {

        ClassInfo declaringClass = producerField.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + UNDERSCORE
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        Type providerType = bean.getProviderType();
        String baseName = declaringClassBase + PRODUCER_FIELD_SUFFIX + UNDERSCORE + producerField.name();
        String providerTypeName = providerType.name().toString();
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name());
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null);

        // Foo_Bean implements InjectableBean<T>
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableBean.class, Supplier.class).build();

        // Fields
        FieldCreator beanTypes = beanCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator qualifiers = null;
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            qualifiers = beanCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        if (bean.getScope().isNormal()) {
            // For normal scopes a client proxy is generated too
            beanCreator.getFieldCreator(FIELD_NAME_PROXY, getProxyTypeName(bean, baseName))
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        createProviderFields(beanCreator, bean, Collections.emptyMap(), Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, baseName, Collections.emptyMap(), Collections.emptyMap(),
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (!bean.hasDefaultDestroy()) {
            implementDestroy(bean, beanCreator, providerTypeName, null, reflectionRegistration, isApplicationClass, baseName);
        }
        implementCreate(classOutput, beanCreator, bean, providerTypeName, baseName, Collections.emptyMap(),
                Collections.emptyMap(), reflectionRegistration,
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerTypeName, baseName);

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
        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }

        beanCreator.close();
        return classOutput.getResources();
    }

    protected void initMaps(BeanInfo bean, Map<InjectionPointInfo, String> injectionPointToProvider,
            Map<InterceptorInfo, String> interceptorToProvider) {
        int providerIdx = 1;
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            injectionPointToProvider.put(injectionPoint, "injectProviderSupplier" + providerIdx++);
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                injectionPointToProvider.put(injectionPoint, "disposerProviderSupplier" + providerIdx++);
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            interceptorToProvider.put(interceptor, "interceptorProviderSupplier" + providerIdx++);
        }
    }

    protected void createProviderFields(ClassCreator beanCreator, BeanInfo bean,
            Map<InjectionPointInfo, String> injectionPointToProviderSupplier,
            Map<InterceptorInfo, String> interceptorToProviderSupplier) {
        // Declaring bean provider
        if (bean.isProducerMethod() || bean.isProducerField()) {
            beanCreator.getFieldCreator(FIELD_NAME_DECLARING_PROVIDER_SUPPLIER, Supplier.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        // Injection points
        for (String provider : injectionPointToProviderSupplier.values()) {
            beanCreator.getFieldCreator(provider, Supplier.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
        // Interceptors
        for (String interceptorProvider : interceptorToProviderSupplier.values()) {
            beanCreator.getFieldCreator(interceptorProvider, Supplier.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {
        initConstructor(classOutput, beanCreator, bean, baseName, injectionPointToProviderField, interceptorToProviderField,
                annotationLiterals, reflectionRegistration)
                        .returnValue(null);
    }

    protected MethodCreator initConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, String baseName,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {

        // First collect all param types
        List<String> parameterTypes = new ArrayList<>();
        if (bean.isProducerMethod() || bean.isProducerField()) {
            parameterTypes.add(Supplier.class.getName());
        }
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            if (BuiltinBean.resolve(injectionPoint) == null) {
                parameterTypes.add(Supplier.class.getName());
            }
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                if (BuiltinBean.resolve(injectionPoint) == null) {
                    parameterTypes.add(Supplier.class.getName());
                }
            }
        }
        for (int i = 0; i < interceptorToProviderField.size(); i++) {
            parameterTypes.add(Supplier.class.getName());
        }

        MethodCreator constructor = beanCreator.getMethodCreator(Methods.INIT, "V", parameterTypes.toArray(new String[0]));
        // Invoke super()
        constructor.invokeSpecialMethod(MethodDescriptors.OBJECT_CONSTRUCTOR, constructor.getThis());

        // Get the TCCL - we will use it later 
        ResultHandle currentThread = constructor
                .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
        ResultHandle tccl = constructor.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);

        // Declaring bean provider
        int paramIdx = 0;
        if (bean.isProducerMethod() || bean.isProducerField()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                            Supplier.class.getName()),
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
                builtinBean.getGenerator()
                        .generate(new GeneratorContext(classOutput, bean.getDeployment(), injectionPoint, beanCreator,
                                constructor, injectionPointToProviderField.get(injectionPoint), annotationLiterals, bean,
                                reflectionRegistration));
            } else {

                if (BuiltinScope.DEPENDENT.is(injectionPoint.getResolvedBean().getScope()) && (injectionPoint.getResolvedBean()
                        .getAllInjectionPoints().stream()
                        .anyMatch(ip -> BuiltinBean.INJECTION_POINT.hasRawTypeDotName(ip.getRequiredType().name()))
                        || injectionPoint.getResolvedBean().isSynthetic())) {
                    // Injection point resolves to a dependent bean that injects InjectionPoint metadata and so we need to wrap the injectable
                    // reference provider
                    ResultHandle wrapHandle = wrapCurrentInjectionPoint(classOutput, beanCreator, bean, constructor,
                            injectionPoint, paramIdx++, tccl, reflectionRegistration);
                    ResultHandle wrapSupplierHandle = constructor.newInstance(
                            MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, wrapHandle);
                    constructor.writeInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                                    Supplier.class.getName()),
                            constructor.getThis(), wrapSupplierHandle);
                } else {
                    constructor.writeInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                                    Supplier.class.getName()),
                            constructor.getThis(), constructor.getMethodParam(paramIdx++));
                }
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                            Supplier.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(paramIdx++));
        }

        // Bean types
        ResultHandle typesHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (org.jboss.jandex.Type type : bean.getTypes()) {
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, typesHandle,
                    Types.getTypeHandle(constructor, type, tccl));
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
            // this.proxy = new Bar_ClientProxy(this)
            String proxyTypeName = getProxyTypeName(bean, baseName);
            constructor.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_PROXY, proxyTypeName),
                    constructor.getThis(),
                    constructor.newInstance(
                            MethodDescriptor.ofConstructor(proxyTypeName, beanCreator.getClassName()), constructor.getThis()));
        }

        return constructor;
    }

    protected void implementDestroy(BeanInfo bean, ClassCreator beanCreator, String providerTypeName,
            Map<InjectionPointInfo, String> injectionPointToProviderField, ReflectionRegistration reflectionRegistration,
            boolean isApplicationClass, String baseName) {

        MethodCreator destroy = beanCreator.getMethodCreator("destroy", void.class, providerTypeName, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (bean.isClassBean()) {
            if (!bean.isInterceptor()) {
                // PreDestroy interceptors
                if (!bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()) {
                    destroy.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName),
                                    SubclassGenerator.DESTROY_METHOD_NAME,
                                    void.class),
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

            ResultHandle declaringProviderSupplierHandle = destroy.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                            Supplier.class.getName()),
                    destroy.getThis());
            ResultHandle declaringProviderHandle = destroy.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle);
            ResultHandle ctxHandle = destroy.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), destroy.loadNull());
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
                    ResultHandle childCtxHandle = destroy.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                            declaringProviderHandle, ctxHandle);
                    ResultHandle providerSupplierHandle = destroy
                            .readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                    injectionPointToProviderField.get(injectionPointsIterator.next()),
                                    Supplier.class.getName()), destroy.getThis());
                    ResultHandle providerHandle = destroy.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                            providerSupplierHandle);
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
            Map<InjectionPointInfo, String> injectionPointToProviderSupplierField,
            Map<InterceptorInfo, String> interceptorToProviderSupplierField,
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

                // instances of around/post construct interceptors also need to be shared
                // build a map that links InterceptorInfo to ResultHandle and reuse that when creating wrappers
                Map<InterceptorInfo, ResultHandle> interceptorToResultHandle = new HashMap<>();

                for (InterceptorInfo interceptor : wraps) {
                    ResultHandle interceptorProviderSupplier = create.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderSupplierField.get(interceptor),
                                    Supplier.class.getName()),
                            create.getThis());
                    ResultHandle interceptorProvider = create.invokeInterfaceMethod(
                            MethodDescriptors.SUPPLIER_GET, interceptorProviderSupplier);
                    ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD,
                            create.getMethodParam(0));
                    ResultHandle interceptorInstanceHandle = create.invokeInterfaceMethod(
                            MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, interceptorProvider,
                            childCtxHandle);
                    interceptorToResultHandle.put(interceptor, interceptorInstanceHandle);
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
                        ResultHandle interceptorSupplierHandle = create.readInstanceField(
                                FieldDescriptor.of(beanCreator.getClassName(),
                                        interceptorToProviderSupplierField.get(interceptor), Supplier.class.getName()),
                                create.getThis());
                        ResultHandle interceptorHandle = create.invokeInterfaceMethod(
                                MethodDescriptors.SUPPLIER_GET, interceptorSupplierHandle);

                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_POST_CONSTRUCT,
                                interceptorHandle, interceptorToResultHandle.get(interceptor));

                        // postConstructs.add(InterceptorInvocation.postConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                        create.invokeInterfaceMethod(MethodDescriptors.LIST_ADD, postConstructsHandle,
                                interceptorInvocationHandle);
                    }
                }
                if (!aroundConstructs.isEmpty()) {
                    // aroundConstructs = new ArrayList<InterceptorInvocation>()
                    aroundConstructsHandle = create.newInstance(MethodDescriptor.ofConstructor(ArrayList.class));
                    for (InterceptorInfo interceptor : aroundConstructs.interceptors) {
                        ResultHandle interceptorSupplierHandle = create.readInstanceField(
                                FieldDescriptor.of(beanCreator.getClassName(),
                                        interceptorToProviderSupplierField.get(interceptor), Supplier.class.getName()),
                                create.getThis());
                        ResultHandle interceptorHandle = create.invokeInterfaceMethod(
                                MethodDescriptors.SUPPLIER_GET, interceptorSupplierHandle);

                        ResultHandle interceptorInvocationHandle = create.invokeStaticMethod(
                                MethodDescriptors.INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT,
                                interceptorHandle, interceptorToResultHandle.get(interceptor));

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
                    reflectionRegistration.registerMethod(constructorInjection.get().target.asMethod());
                } else {
                    // constructor = Reflections.findConstructor(Foo.class)
                    ResultHandle[] paramsHandles = new ResultHandle[2];
                    paramsHandles[0] = create.loadClass(providerTypeName);
                    paramsHandles[1] = create.newArray(Class.class, create.load(0));
                    constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                            paramsHandles);
                    MethodInfo noArgsConstructor = bean.getTarget().get().asClass().method(Methods.INIT);
                    reflectionRegistration.registerMethod(noArgsConstructor);
                }

                List<ResultHandle> providerHandles = newProviderHandles(bean, beanCreator, create,
                        injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
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

                ResultHandle invocationContextHandle = create.invokeStaticMethod(
                        MethodDescriptors.INVOCATION_CONTEXTS_AROUND_CONSTRUCT, constructorHandle,
                        aroundConstructsHandle, func.getInstance(), bindingsHandle);
                TryBlock tryCatch = create.tryBlock();
                CatchBlockCreator exceptionCatch = tryCatch.addCatch(Exception.class);
                // throw new RuntimeException(e)
                exceptionCatch.throwException(RuntimeException.class, "Error invoking aroundConstructs",
                        exceptionCatch.getCaughtException());
                // InvocationContextImpl.aroundConstruct(constructor,aroundConstructs,forward).proceed()
                tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_PROCEED,
                        invocationContextHandle);
                // instance = InvocationContext.getTarget()
                tryCatch.assign(instanceHandle, tryCatch.invokeInterfaceMethod(MethodDescriptors.INVOCATION_CONTEXT_GET_TARGET,
                        invocationContextHandle));

            } else {
                create.assign(instanceHandle, newInstanceHandle(bean, beanCreator, create, create, providerTypeName, baseName,
                        newProviderHandles(bean, beanCreator, create, injectionPointToProviderSupplierField,
                                interceptorToProviderSupplierField,
                                interceptorToWrap),
                        reflectionRegistration, isApplicationClass));
            }

            // Perform field and initializer injections
            for (Injection fieldInjection : fieldInjections) {
                TryBlock tryBlock = create.tryBlock();
                InjectionPointInfo injectionPoint = fieldInjection.injectionPoints.get(0);
                ResultHandle providerSupplierHandle = tryBlock.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                        tryBlock.getThis());
                ResultHandle providerHandle = tryBlock.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, providerSupplierHandle);
                ResultHandle childCtxHandle = tryBlock.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                        providerHandle, tryBlock.getMethodParam(0));
                ResultHandle referenceHandle = tryBlock.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle);

                FieldInfo injectedField = fieldInjection.target.asField();
                if (isReflectionFallbackNeeded(injectedField, targetPackage)) {
                    if (Modifier.isPrivate(injectedField.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@Inject field %s#%s", fieldInjection.target.asField().declaringClass().name(),
                                        fieldInjection.target.asField().name()));
                    }
                    reflectionRegistration.registerField(injectedField);
                    tryBlock.invokeStaticMethod(MethodDescriptors.REFLECTIONS_WRITE_FIELD,
                            tryBlock.loadClass(injectedField.declaringClass().name().toString()),
                            tryBlock.load(injectedField.name()), instanceHandle, referenceHandle);

                } else {
                    // We cannot use injectionPoint.getRequiredType() because it might be a resolved parameterize type and we could get NoSuchFieldError
                    String fieldType = injectionPoint.getTarget().asField().type().name().toString();
                    tryBlock.writeInstanceField(
                            FieldDescriptor.of(providerTypeName, injectedField.name(),
                                    fieldType),
                            instanceHandle, referenceHandle);
                }
                CatchBlockCreator catchBlock = tryBlock.addCatch(RuntimeException.class);
                catchBlock.throwException(RuntimeException.class, "Error injecting " + fieldInjection.target,
                        catchBlock.getCaughtException());
            }
            for (Injection methodInjection : methodInjections) {
                ResultHandle[] referenceHandles = new ResultHandle[methodInjection.injectionPoints.size()];
                int paramIdx = 0;
                for (InjectionPointInfo injectionPoint : methodInjection.injectionPoints) {
                    ResultHandle providerSupplierHandle = create.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(),
                                    injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                            create.getThis());
                    ResultHandle providerHandle = create.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                            providerSupplierHandle);
                    ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                            providerHandle, create.getMethodParam(0));
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
                        MethodDescriptors.INVOCATION_CONTEXTS_POST_CONSTRUCT, instanceHandle,
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
            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerTypeName));
            // instance = declaringProviderSupplier.get().get(new CreationalContextImpl<>()).produce()
            ResultHandle ctxHandle = create.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), create.getThis());
            ResultHandle declaringProviderInstanceHandle;
            ResultHandle declaringProviderSupplierHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                            Supplier.class.getName()),
                    create.getThis());
            ResultHandle declaringProviderHandle = create.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle);
            if (Modifier.isStatic(producerMethod.flags())) {
                // for static producers, we don't need to resolve this this handle
                declaringProviderInstanceHandle = create.loadNull();
            } else {
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                        MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                        ctxHandle);
                if (bean.getDeclaringBean().getScope().isNormal()) {
                    // We need to unwrap the client proxy
                    declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                            MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                            declaringProviderInstanceHandle);
                }
            }

            List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
            ResultHandle[] referenceHandles = new ResultHandle[injectionPoints.size()];
            int paramIdx = 0;
            for (InjectionPointInfo injectionPoint : injectionPoints) {
                ResultHandle providerSupplierHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                        create.getThis());
                ResultHandle providerHandle = create.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                        providerSupplierHandle);
                ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                        providerHandle, create.getMethodParam(0));
                ResultHandle referenceHandle = create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle);
                referenceHandles[paramIdx++] = referenceHandle;
            }

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
                        declaringProviderInstanceHandle,
                        argsArray));
            } else {
                ResultHandle invokeMethodHandle;
                if (Modifier.isStatic(producerMethod.flags())) {
                    invokeMethodHandle = create.invokeStaticMethod(MethodDescriptor.of(producerMethod),
                            referenceHandles);
                } else {
                    invokeMethodHandle = create.invokeVirtualMethod(MethodDescriptor.of(producerMethod),
                            declaringProviderInstanceHandle, referenceHandles);
                }
                create.assign(instanceHandle, invokeMethodHandle);
            }

            if (bean.getScope().isNormal()) {
                create.ifNull(instanceHandle).trueBranch().throwException(IllegalProductException.class,
                        "Normal scoped producer method may not return null: " + bean.getDeclaringBean().getImplClazz().name()
                                + "." +
                                bean.getTarget().get().asMethod().name() + "()");
            }

            // If the declaring bean is @Dependent we must destroy the instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope())) {
                create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                        declaringProviderInstanceHandle, ctxHandle);
            }
            create.returnValue(instanceHandle);

        } else if (bean.isProducerField()) {
            instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerTypeName));
            // instance = declaringProviderSupplier.get().get(new CreationalContextImpl<>()).field

            FieldInfo producerField = bean.getTarget().get().asField();

            ResultHandle declaringProviderSupplierHandle = create.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                            Supplier.class.getName()),
                    create.getThis());
            ResultHandle declaringProviderHandle = create.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle);
            ResultHandle ctxHandle = create.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), create.getThis());
            ResultHandle declaringProviderInstanceHandle;
            if (Modifier.isStatic(producerField.flags())) {
                declaringProviderInstanceHandle = create.loadNull();
            } else {
                declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                        MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                        ctxHandle);

                if (bean.getDeclaringBean().getScope().isNormal()) {
                    // We need to unwrap the client proxy
                    declaringProviderInstanceHandle = create.invokeInterfaceMethod(
                            MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                            declaringProviderInstanceHandle);
                }
            }

            if (Modifier.isPrivate(producerField.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Producer field %s#%s", producerField.declaringClass().name(), producerField.name()));
                reflectionRegistration.registerField(producerField);
                create.assign(instanceHandle, create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_READ_FIELD,
                        create.loadClass(producerField.declaringClass().name().toString()), create.load(producerField.name()),
                        declaringProviderInstanceHandle));
            } else {
                ResultHandle readFieldHandle;
                if (Modifier.isStatic(producerField.flags())) {
                    readFieldHandle = create.readStaticField(FieldDescriptor.of(producerField));
                } else {
                    readFieldHandle = create.readInstanceField(FieldDescriptor.of(producerField),
                            declaringProviderInstanceHandle);
                }
                create.assign(instanceHandle, readFieldHandle);
            }

            if (bean.getScope().isNormal()) {
                create.ifNull(instanceHandle).trueBranch().throwException(IllegalProductException.class,
                        "Normal scoped producer field may not be null: " + bean.getDeclaringBean().getImplClazz().name() + "."
                                + bean.getTarget().get().asField().name());
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
                ResultHandle providerSupplierHandle = createMethod.readInstanceField(
                        FieldDescriptor.of(beanCreator.getClassName(),
                                injectionPointToProviderField.get(injectionPoint), Supplier.class.getName()),
                        createMethod.getThis());
                ResultHandle providerHandle = createMethod.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                        providerSupplierHandle);
                ResultHandle childCtx = createMethod.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                        providerHandle, createMethod.getMethodParam(0));
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
                    ResultHandle interceptorProviderSupplierHandle = createMethod.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                                    Supplier.class),
                            createMethod.getThis());
                    ResultHandle interceptorProviderHandle = createMethod.invokeInterfaceMethod(
                            MethodDescriptors.SUPPLIER_GET, interceptorProviderSupplierHandle);
                    providerHandles.add(interceptorProviderHandle);
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
    protected void implementGet(BeanInfo bean, ClassCreator beanCreator, String providerTypeName, String baseName) {

        MethodCreator get = beanCreator.getMethodCreator("get", providerTypeName, CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            // Foo instance = create(ctx)
            ResultHandle instance = get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), "create", providerTypeName, CreationalContext.class),
                    get.getThis(),
                    get.getMethodParam(0));

            // We can optimize if:
            // 1) class bean - has no @PreDestroy interceptor and there is no @PreDestroy callback
            // 2) producer - there is no disposal method
            boolean canBeOptimized = false;
            if (bean.isClassBean()) {
                canBeOptimized = bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()
                        && Beans.getCallbacks(bean.getTarget().get().asClass(),
                                DotNames.PRE_DESTROY,
                                bean.getDeployment().getIndex()).isEmpty();
            } else if (bean.isProducerMethod() || bean.isProducerField()) {
                canBeOptimized = bean.getDisposer() == null;
            }

            if (canBeOptimized) {
                // If there is no dependency in the creational context we don't have to store the instance in the CreationalContext
                ResultHandle creationalContext = get.checkCast(get.getMethodParam(0), CreationalContextImpl.class);
                get.ifNonZero(
                        get.invokeVirtualMethod(MethodDescriptors.CREATIONAL_CTX_HAS_DEPENDENT_INSTANCES, creationalContext))
                        .falseBranch().returnValue(instance);
            }

            // CreationalContextImpl.addDependencyToParent(this,instance,ctx)
            get.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_ADD_DEP_TO_PARENT, get.getThis(), instance,
                    get.getMethodParam(0));
            // return instance
            get.returnValue(instance);
        } else if (BuiltinScope.SINGLETON.is(bean.getScope())) {
            // return Arc.container().getContext(getScope()).get(this, new CreationalContextImpl<>())
            ResultHandle container = get.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
            ResultHandle creationalContext = get.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                    get.getThis());
            ResultHandle scope = get.loadClass(bean.getScope().getDotName().toString());
            ResultHandle context = get.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT, container,
                    scope);
            get.returnValue(
                    get.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET, context, get.getThis(), creationalContext));
        } else if (bean.getScope().isNormal()) {
            // return proxy.get()
            ResultHandle proxy = get.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_PROXY, getProxyTypeName(bean, baseName)),
                    get.getThis());
            get.returnValue(proxy);
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
        ResultHandle declaringProviderSupplierHandle = getDeclaringBean.readInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                        Supplier.class.getName()),
                getDeclaringBean.getThis());
        getDeclaringBean.returnValue(getDeclaringBean.invokeInterfaceMethod(
                MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle));
    }

    protected void implementGetAlternativePriority(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getAlternativePriority = beanCreator.getMethodCreator("getAlternativePriority", Integer.class)
                .setModifiers(ACC_PUBLIC);
        getAlternativePriority
                .returnValue(getAlternativePriority.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                        getAlternativePriority.load(bean.getAlternativePriority())));
    }

    protected void implementIsDefaultBean(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator isDefaultBean = beanCreator.getMethodCreator("isDefaultBean", boolean.class)
                .setModifiers(ACC_PUBLIC);
        isDefaultBean
                .returnValue(isDefaultBean.load(bean.isDefaultBean()));
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

    protected void implementSupplierGet(ClassCreator beanCreator) {
        MethodCreator get = beanCreator.getMethodCreator("get", Object.class).setModifiers(ACC_PUBLIC);
        get.returnValue(get.getThis());
    }

    private String getProxyTypeName(BeanInfo bean, String baseName) {
        return getPackageName(bean) + "." + baseName + ClientProxyGenerator.CLIENT_PROXY_SUFFIX;
    }

    private ResultHandle wrapCurrentInjectionPoint(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            MethodCreator constructor, InjectionPointInfo injectionPoint, int paramIdx, ResultHandle tccl,
            ReflectionRegistration reflectionRegistration) {
        ResultHandle requiredQualifiersHandle = collectQualifiers(classOutput, beanCreator, bean.getDeployment(), constructor,
                injectionPoint,
                annotationLiterals);
        ResultHandle annotationsHandle = collectAnnotations(classOutput, beanCreator, bean.getDeployment(), constructor,
                injectionPoint, annotationLiterals);
        ResultHandle javaMemberHandle = getJavaMemberHandle(constructor, injectionPoint, reflectionRegistration);

        return constructor.newInstance(
                MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableBean.class,
                        Supplier.class, java.lang.reflect.Type.class,
                        Set.class, Set.class, Member.class, int.class),
                constructor.getThis(), constructor.getMethodParam(paramIdx),
                Types.getTypeHandle(constructor, injectionPoint.getRequiredType(), tccl),
                requiredQualifiersHandle, annotationsHandle, javaMemberHandle, constructor.load(injectionPoint.getPosition()));
    }

    static ResultHandle getJavaMemberHandle(MethodCreator constructor,
            InjectionPointInfo injectionPoint, ReflectionRegistration reflectionRegistration) {
        ResultHandle javaMemberHandle;
        if (Kind.FIELD.equals(injectionPoint.getTarget().kind())) {
            FieldInfo field = injectionPoint.getTarget().asField();
            javaMemberHandle = constructor.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_FIELD,
                    constructor.loadClass(field.declaringClass().name().toString()),
                    constructor.load(field.name()));
            reflectionRegistration.registerField(field);
        } else {
            MethodInfo method = injectionPoint.getTarget().asMethod();
            reflectionRegistration.registerMethod(method);
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
            annotations = Annotations.getParameterAnnotations(beanDeployment,
                    method, injectionPoint.getPosition());
        }
        for (AnnotationInstance annotation : annotations) {
            if (DotNames.INJECT.equals(annotation.name())) {
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotationsHandle,
                        constructor.readStaticField(FieldDescriptor.of(InjectLiteral.class, "INSTANCE", InjectLiteral.class)));
            } else {
                // Create annotation literal if needed
                ClassInfo literalClass = getClassByName(beanDeployment.getIndex(), annotation.name());
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
        ResultHandle requiredQualifiersHandle;
        if (injectionPoint.hasDefaultedQualifier()) {
            requiredQualifiersHandle = constructor.readStaticField(FieldDescriptors.QUALIFIERS_IP_QUALIFIERS);
        } else {
            requiredQualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
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
        }
        return requiredQualifiersHandle;
    }

}
