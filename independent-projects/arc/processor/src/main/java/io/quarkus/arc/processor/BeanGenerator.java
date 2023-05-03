package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;

import java.lang.annotation.Annotation;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.CurrentInjectionPointProvider;
import io.quarkus.arc.impl.DecoratorDelegateProvider;
import io.quarkus.arc.impl.InitializedInterceptor;
import io.quarkus.arc.impl.SyntheticCreationalContextImpl;
import io.quarkus.arc.impl.SyntheticCreationalContextImpl.TypeAndQualifiers;
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
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.Gizmo.StringBuilderGenerator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

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

    protected final AnnotationLiteralProcessor annotationLiterals;
    protected final Predicate<DotName> applicationClassPredicate;
    protected final PrivateMembersCollector privateMembers;
    protected final Set<String> existingClasses;
    protected final Map<BeanInfo, String> beanToGeneratedName;
    protected final Map<BeanInfo, String> beanToGeneratedBaseName;
    protected final Predicate<DotName> injectionPointAnnotationsPredicate;
    protected final List<Function<BeanInfo, Consumer<BytecodeCreator>>> suppressConditionGenerators;

    public BeanGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate,
            List<Function<BeanInfo, Consumer<BytecodeCreator>>> suppressConditionGenerators) {
        super(generateSources, reflectionRegistration);
        this.annotationLiterals = annotationLiterals;
        this.applicationClassPredicate = applicationClassPredicate;
        this.privateMembers = privateMembers;
        this.existingClasses = existingClasses;
        this.beanToGeneratedName = beanToGeneratedName;
        this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;
        this.suppressConditionGenerators = suppressConditionGenerators;
        this.beanToGeneratedBaseName = new HashMap<>();
    }

    /**
     *
     * @param bean
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean) {
        if (bean.getTarget().isPresent()) {
            AnnotationTarget target = bean.getTarget().get();
            switch (target.kind()) {
                case CLASS:
                    return generateClassBean(bean, target.asClass());
                case METHOD:
                    return generateProducerMethodBean(bean, target.asMethod());
                case FIELD:
                    return generateProducerFieldBean(bean, target.asField());
                default:
                    throw new IllegalArgumentException("Unsupported bean type");
            }
        } else {
            // Synthetic beans
            return generateSyntheticBean(bean);
        }
    }

    /**
     * Precompute the generated name for the given bean so that the {@link ComponentsProviderGenerator} can be executed before
     * all beans metadata are generated.
     *
     * @param bean
     */
    void precomputeGeneratedName(BeanInfo bean) {
        if (bean.isSynthetic()) {
            generateSyntheticBeanName(bean);
        } else if (bean.isProducerField()) {
            generateProducerFieldBeanName(bean);
        } else if (bean.isProducerMethod()) {
            generateProducerMethodBeanName(bean);
        } else if (bean.isClassBean()) {
            generateClassBeanName(bean);
        }
    }

    private void generateProducerFieldBeanName(BeanInfo bean) {
        FieldInfo producerField = bean.getTarget().get().asField();
        ClassInfo declaringClass = producerField.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + UNDERSCORE
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        String baseName = declaringClassBase + PRODUCER_FIELD_SUFFIX + UNDERSCORE + producerField.name();
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateProducerMethodBeanName(BeanInfo bean) {
        MethodInfo producerMethod = bean.getTarget().get().asMethod();
        ClassInfo declaringClass = producerMethod.declaringClass();
        String declaringClassBase;
        if (declaringClass.enclosingClass() != null) {
            declaringClassBase = DotNames.simpleName(declaringClass.enclosingClass()) + UNDERSCORE
                    + DotNames.simpleName(declaringClass);
        } else {
            declaringClassBase = DotNames.simpleName(declaringClass);
        }

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(producerMethod.name())
                .append(UNDERSCORE)
                .append(producerMethod.returnType().name().toString());

        for (Type i : producerMethod.parameterTypes()) {
            sigBuilder.append(i.name().toString());
        }

        String baseName = declaringClassBase + PRODUCER_METHOD_SUFFIX + UNDERSCORE + producerMethod.name() + UNDERSCORE
                + Hashes.sha1(sigBuilder.toString());
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateClassBeanName(BeanInfo bean) {
        ClassInfo beanClass = bean.getTarget().get().asClass();
        String baseName;
        if (beanClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(beanClass.enclosingClass()) + UNDERSCORE + DotNames.simpleName(beanClass);
        } else {
            baseName = DotNames.simpleName(beanClass);
        }
        this.beanToGeneratedBaseName.put(bean, baseName);
        ProviderType providerType = new ProviderType(bean.getProviderType());
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateSyntheticBeanName(BeanInfo bean) {
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
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = bean.getTargetPackageName();
        this.beanToGeneratedName.put(bean, generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX));
    }

    Collection<Resource> generateSyntheticBean(BeanInfo bean) {
        ProviderType providerType = new ProviderType(bean.getProviderType());
        String targetPackage = bean.getTargetPackageName();
        String baseName = beanToGeneratedBaseName.get(bean);
        String generatedName = beanToGeneratedName.get(bean);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(bean.getImplClazz().name())
                || bean.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null, generateSources);

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
            initializeProxy(bean, baseName, beanCreator);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderSupplierField = Collections.emptyMap();
        if (bean.hasInjectionPoint()) {
            injectionPointToProviderSupplierField = new HashMap<>();
            // Synthetic beans are not intercepted
            initMaps(bean, injectionPointToProviderSupplierField, null, null);
            createProviderFields(beanCreator, bean, injectionPointToProviderSupplierField, Collections.emptyMap(),
                    Collections.emptyMap());
        }

        MethodCreator constructor = initConstructor(classOutput, beanCreator, bean, injectionPointToProviderSupplierField,
                Collections.emptyMap(), Collections.emptyMap(),
                annotationLiterals, reflectionRegistration);

        SyntheticComponentsUtil.addParamsFieldAndInit(beanCreator, constructor, bean.getParams(), annotationLiterals,
                bean.getDeployment().getBeanArchiveIndex());

        constructor.returnValue(null);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (bean.hasDestroyLogic()) {
            implementDestroy(bean, beanCreator, providerType, Collections.emptyMap(), isApplicationClass, baseName,
                    targetPackage);
        }
        implementCreate(classOutput, beanCreator, bean, providerType, baseName,
                injectionPointToProviderSupplierField, Collections.emptyMap(),
                Collections.emptyMap(), targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerType, baseName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }

        implementIsAlternative(bean, beanCreator);
        implementGetPriority(bean, beanCreator);

        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetImplementationClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }
        implementGetKind(beanCreator, InjectableBean.Kind.SYNTHETIC);
        implementEquals(bean, beanCreator);
        implementHashCode(bean, beanCreator);
        implementToString(beanCreator);

        implementGetInjectionPoints(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateClassBean(BeanInfo bean, ClassInfo beanClass) {
        ProviderType providerType = new ProviderType(bean.getProviderType());
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = beanToGeneratedName.get(bean);
        String baseName = beanToGeneratedBaseName.get(bean);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(beanClass.name()) || bean.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null, generateSources);

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
            initializeProxy(bean, baseName, beanCreator);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderSupplierField = new HashMap<>();
        Map<InterceptorInfo, String> interceptorToProviderSupplierField = new HashMap<>();
        Map<DecoratorInfo, String> decoratorToProviderSupplierField = new HashMap<>();
        initMaps(bean, injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                decoratorToProviderSupplierField);

        createProviderFields(beanCreator, bean, injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                decoratorToProviderSupplierField);
        createConstructor(classOutput, beanCreator, bean, injectionPointToProviderSupplierField,
                interceptorToProviderSupplierField, decoratorToProviderSupplierField,
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (bean.hasDestroyLogic()) {
            implementDestroy(bean, beanCreator, providerType, injectionPointToProviderSupplierField, isApplicationClass,
                    baseName, targetPackage);
        }
        implementCreate(classOutput, beanCreator, bean, providerType, baseName,
                injectionPointToProviderSupplierField,
                interceptorToProviderSupplierField,
                decoratorToProviderSupplierField,
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerType, baseName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }

        implementIsAlternative(bean, beanCreator);
        implementGetPriority(bean, beanCreator);

        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }

        implementIsSuppressed(bean, beanCreator);
        implementEquals(bean, beanCreator);
        implementHashCode(bean, beanCreator);
        implementToString(beanCreator);

        implementGetInjectionPoints(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerMethodBean(BeanInfo bean, MethodInfo producerMethod) {
        ClassInfo declaringClass = producerMethod.declaringClass();
        ProviderType providerType = new ProviderType(bean.getProviderType());
        String baseName = beanToGeneratedBaseName.get(bean);
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = beanToGeneratedName.get(bean);

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name()) || bean.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null, generateSources);

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
            initializeProxy(bean, baseName, beanCreator);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        // Producer methods are not intercepted
        initMaps(bean, injectionPointToProviderField, null, null);

        createProviderFields(beanCreator, bean, injectionPointToProviderField, Collections.emptyMap(), Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, injectionPointToProviderField, Collections.emptyMap(),
                Collections.emptyMap(),
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (bean.hasDestroyLogic()) {
            implementDestroy(bean, beanCreator, providerType, injectionPointToProviderField, isApplicationClass, baseName,
                    targetPackage);
        }
        implementCreate(classOutput, beanCreator, bean, providerType, baseName,
                injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(),
                targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerType, baseName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }

        implementIsAlternative(bean, beanCreator);
        implementGetPriority(bean, beanCreator);

        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetImplementationClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }
        implementGetKind(beanCreator, InjectableBean.Kind.PRODUCER_METHOD);
        implementIsSuppressed(bean, beanCreator);
        implementEquals(bean, beanCreator);
        implementHashCode(bean, beanCreator);
        implementToString(beanCreator);

        implementGetInjectionPoints(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    Collection<Resource> generateProducerFieldBean(BeanInfo bean, FieldInfo producerField) {
        ClassInfo declaringClass = producerField.declaringClass();
        ProviderType providerType = new ProviderType(bean.getProviderType());
        String baseName = beanToGeneratedBaseName.get(bean);
        String targetPackage = DotNames.packageName(declaringClass.name());
        String generatedName = beanToGeneratedName.get(bean);

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(declaringClass.name()) || bean.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null, generateSources);

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
            initializeProxy(bean, baseName, beanCreator);
        }
        FieldCreator stereotypes = null;
        if (!bean.getStereotypes().isEmpty()) {
            stereotypes = beanCreator.getFieldCreator(FIELD_NAME_STEREOTYPES, Set.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        createProviderFields(beanCreator, bean, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        createConstructor(classOutput, beanCreator, bean, Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(),
                annotationLiterals, reflectionRegistration);

        implementGetIdentifier(bean, beanCreator);
        implementSupplierGet(beanCreator);
        if (bean.hasDestroyLogic()) {
            implementDestroy(bean, beanCreator, providerType, null, isApplicationClass, baseName, targetPackage);
        }
        implementCreate(classOutput, beanCreator, bean, providerType, baseName,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), targetPackage, isApplicationClass);
        implementGet(bean, beanCreator, providerType, baseName);

        implementGetTypes(beanCreator, beanTypes.getFieldDescriptor());
        if (!BuiltinScope.isDefault(bean.getScope())) {
            implementGetScope(bean, beanCreator);
        }
        if (qualifiers != null) {
            implementGetQualifiers(bean, beanCreator, qualifiers.getFieldDescriptor());
        }

        implementIsAlternative(bean, beanCreator);
        implementGetPriority(bean, beanCreator);

        implementGetDeclaringBean(beanCreator);
        if (stereotypes != null) {
            implementGetStereotypes(bean, beanCreator, stereotypes.getFieldDescriptor());
        }
        implementGetBeanClass(bean, beanCreator);
        implementGetImplementationClass(bean, beanCreator);
        implementGetName(bean, beanCreator);
        if (bean.isDefaultBean()) {
            implementIsDefaultBean(bean, beanCreator);
        }
        implementGetKind(beanCreator, InjectableBean.Kind.PRODUCER_FIELD);
        implementIsSuppressed(bean, beanCreator);
        implementEquals(bean, beanCreator);
        implementHashCode(bean, beanCreator);
        implementToString(beanCreator);

        implementGetInjectionPoints(bean, beanCreator);

        beanCreator.close();
        return classOutput.getResources();
    }

    protected void initMaps(BeanInfo bean, Map<InjectionPointInfo, String> injectionPointToProvider,
            Map<InterceptorInfo, String> interceptorToProvider, Map<DecoratorInfo, String> decoratorToProvider) {
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
        for (DecoratorInfo decorator : bean.getBoundDecorators()) {
            decoratorToProvider.put(decorator, "decoratorProviderSupplier" + providerIdx++);
        }
    }

    protected void createProviderFields(ClassCreator beanCreator, BeanInfo bean,
            Map<InjectionPointInfo, String> injectionPointToProviderSupplier,
            Map<InterceptorInfo, String> interceptorToProviderSupplier,
            Map<DecoratorInfo, String> decoratorToProviderSupplier) {
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
        // Decorators
        for (String decoratorProvider : decoratorToProviderSupplier.values()) {
            beanCreator.getFieldCreator(decoratorProvider, Supplier.class).setModifiers(ACC_PRIVATE | ACC_FINAL);
        }
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            Map<DecoratorInfo, String> decoratorToProviderSupplierField,
            AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {
        initConstructor(classOutput, beanCreator, bean, injectionPointToProviderField, interceptorToProviderField,
                decoratorToProviderSupplierField,
                annotationLiterals, reflectionRegistration)
                .returnValue(null);
    }

    protected MethodCreator initConstructor(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            Map<DecoratorInfo, String> decoratorToProviderSupplierField,
            AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {

        // First collect all param types
        List<String> parameterTypes = new ArrayList<>();
        if (bean.isProducerMethod() || bean.isProducerField()) {
            parameterTypes.add(Supplier.class.getName());
        }
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            if (!injectionPoint.isDelegate() && BuiltinBean.resolve(injectionPoint) == null) {
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
        for (int i = 0; i < decoratorToProviderSupplierField.size(); i++) {
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
            if (injectionPoint.isDelegate()) {
                // this.delegateProvider = () -> new DecoratorDelegateProvider();
                ResultHandle delegateProvider = constructor.newInstance(
                        MethodDescriptor.ofConstructor(DecoratorDelegateProvider.class));
                ResultHandle delegateProviderSupplier = constructor.newInstance(
                        MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, delegateProvider);
                constructor.writeInstanceField(
                        FieldDescriptor.of(beanCreator.getClassName(), injectionPointToProviderField.get(injectionPoint),
                                Supplier.class.getName()),
                        constructor.getThis(),
                        delegateProviderSupplier);
            } else {
                if (injectionPoint.getResolvedBean() == null) {
                    BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
                    builtinBean.getGenerator()
                            .generate(new GeneratorContext(classOutput, bean.getDeployment(), injectionPoint, beanCreator,
                                    constructor, injectionPointToProviderField.get(injectionPoint), annotationLiterals, bean,
                                    reflectionRegistration, injectionPointAnnotationsPredicate));
                } else {
                    // Not a built-in bean
                    if (injectionPoint.isCurrentInjectionPointWrapperNeeded()) {
                        ResultHandle wrapHandle = wrapCurrentInjectionPoint(bean, constructor,
                                injectionPoint, paramIdx++, tccl, reflectionRegistration);
                        ResultHandle wrapSupplierHandle = constructor.newInstance(
                                MethodDescriptors.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, wrapHandle);
                        constructor.writeInstanceField(
                                FieldDescriptor.of(beanCreator.getClassName(),
                                        injectionPointToProviderField.get(injectionPoint),
                                        Supplier.class.getName()),
                                constructor.getThis(), wrapSupplierHandle);
                    } else {
                        constructor.writeInstanceField(
                                FieldDescriptor.of(beanCreator.getClassName(),
                                        injectionPointToProviderField.get(injectionPoint),
                                        Supplier.class.getName()),
                                constructor.getThis(), constructor.getMethodParam(paramIdx++));
                    }
                }
            }
        }
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                            Supplier.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(paramIdx++));
        }
        for (DecoratorInfo decorator : bean.getBoundDecorators()) {
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), decoratorToProviderSupplierField.get(decorator),
                            Supplier.class.getName()),
                    constructor.getThis(), constructor.getMethodParam(paramIdx++));
        }

        // Bean types
        ResultHandle typesArray = constructor.newArray(Object.class, bean.getTypes().size());
        int typeIndex = 0;
        for (org.jboss.jandex.Type type : bean.getTypes()) {
            ResultHandle typeHandle;
            try {
                typeHandle = Types.getTypeHandle(constructor, type, tccl);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unable to construct the type handle for " + bean + ": " + e.getMessage());
            }
            constructor.writeArrayValue(typesArray, constructor.load(typeIndex++), typeHandle);
        }
        constructor.writeInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_BEAN_TYPES, Set.class.getName()),
                constructor.getThis(),
                constructor.invokeStaticMethod(MethodDescriptors.SETS_OF,
                        typesArray));

        // Qualifiers
        if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
            ResultHandle qualifiersArray = constructor.newArray(Object.class, bean.getQualifiers().size());
            int qualifierIndex = 0;
            for (AnnotationInstance qualifierAnnotation : bean.getQualifiers()) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                if (qualifier != null) {
                    constructor.writeArrayValue(qualifiersArray, constructor.load(qualifierIndex++),
                            qualifier.getLiteralInstance(constructor));
                } else {
                    // Create the annotation literal first
                    ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifierAnnotation.name());
                    constructor.writeArrayValue(qualifiersArray, constructor.load(qualifierIndex++),
                            annotationLiterals.create(constructor, qualifierClass, qualifierAnnotation));
                }
            }
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_QUALIFIERS, Set.class.getName()),
                    constructor.getThis(),
                    constructor.invokeStaticMethod(MethodDescriptors.SETS_OF,
                            qualifiersArray));
        }

        // Stereotypes
        if (!bean.getStereotypes().isEmpty()) {
            ResultHandle stereotypesArray = constructor.newArray(Object.class, bean.getStereotypes().size());
            int stereotypesIndex = 0;
            for (StereotypeInfo stereotype : bean.getStereotypes()) {
                constructor.writeArrayValue(stereotypesArray, constructor.load(stereotypesIndex++),
                        constructor.loadClass(stereotype.getTarget().name().toString()));
            }
            constructor.writeInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_STEREOTYPES, Set.class.getName()),
                    constructor.getThis(),
                    constructor.invokeStaticMethod(MethodDescriptors.SETS_OF,
                            stereotypesArray));
        }
        return constructor;
    }

    protected void implementDestroy(BeanInfo bean, ClassCreator beanCreator, ProviderType providerType,
            Map<InjectionPointInfo, String> injectionPointToProviderField, boolean isApplicationClass, String baseName,
            String targetPackage) {

        MethodCreator destroy = beanCreator
                .getMethodCreator("destroy", void.class, providerType.descriptorName(), CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (bean.isClassBean()) {
            if (!bean.isInterceptor()) {
                // in case someone calls `Bean.destroy()` directly (i.e., they use the low-level CDI API),
                // they may pass us a client proxy
                ResultHandle instance = destroy.invokeStaticInterfaceMethod(MethodDescriptors.CLIENT_PROXY_UNWRAP,
                        destroy.getMethodParam(0));

                // if there's no `@PreDestroy` interceptor, we'll generate code to invoke `@PreDestroy` callbacks
                // directly into the `destroy` method:
                //
                // public void destroy(MyBean var1, CreationalContext var2) {
                //     var1.myPreDestroyCallback();
                //     var2.release();
                // }
                BytecodeCreator preDestroyBytecode = destroy;

                // PreDestroy interceptors
                if (!bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()) {
                    // if there _is_ some `@PreDestroy` interceptor, however, we'll reify the chain of `@PreDestroy`
                    // callbacks into a `Runnable` that we pass into the interceptor chain to be called
                    // by the last `proceed()` call:
                    //
                    // public void destroy(MyBean var1, CreationalContext var2) {
                    //     // this is a `Runnable` that calls `MyBean.myPreDestroyCallback()`
                    //     MyBean_Bean$$function$$2 var3 = new MyBean_Bean$$function$$2(var1);
                    //     ((MyBean_Subclass)var1).arc$destroy((Runnable)var3);
                    //     var2.release();
                    // }
                    FunctionCreator preDestroyForwarder = destroy.createFunction(Runnable.class);
                    preDestroyBytecode = preDestroyForwarder.getBytecode();

                    destroy.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName),
                                    SubclassGenerator.DESTROY_METHOD_NAME, void.class, Runnable.class),
                            instance, preDestroyForwarder.getInstance());
                }

                // PreDestroy callbacks
                // possibly wrapped into Runnable so that PreDestroy interceptors can proceed() correctly
                List<MethodInfo> preDestroyCallbacks = Beans.getCallbacks(bean.getTarget().get().asClass(),
                        DotNames.PRE_DESTROY,
                        bean.getDeployment().getBeanArchiveIndex());
                for (MethodInfo callback : preDestroyCallbacks) {
                    if (isReflectionFallbackNeeded(callback, targetPackage)) {
                        if (Modifier.isPrivate(callback.flags())) {
                            privateMembers.add(isApplicationClass, String.format("@PreDestroy callback %s#%s()",
                                    callback.declaringClass().name(), callback.name()));
                        }
                        reflectionRegistration.registerMethod(callback);
                        preDestroyBytecode.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                                preDestroyBytecode.loadClass(callback.declaringClass().name().toString()),
                                preDestroyBytecode.load(callback.name()),
                                preDestroyBytecode.newArray(Class.class, preDestroyBytecode.load(0)),
                                instance,
                                preDestroyBytecode.newArray(Object.class, preDestroyBytecode.load(0)));
                    } else {
                        // instance.superCoolDestroyCallback()
                        preDestroyBytecode.invokeVirtualMethod(MethodDescriptor.of(callback), instance);
                    }
                }
                if (preDestroyBytecode != destroy) {
                    // only if we're generating a `Runnable`, see above
                    preDestroyBytecode.returnVoid();
                }
            }

            // ctx.release()
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, destroy.getMethodParam(1));
            destroy.returnValue(null);

        } else if (bean.getDisposer() != null) {
            // Invoke the disposer method
            // declaringProvider.get(new CreationalContextImpl<>()).dispose()
            MethodInfo disposerMethod = bean.getDisposer().getDisposerMethod();
            boolean isStatic = Modifier.isStatic(disposerMethod.flags());

            ResultHandle declaringProviderSupplierHandle = destroy.readInstanceField(
                    FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                            Supplier.class.getName()),
                    destroy.getThis());
            ResultHandle declaringProviderHandle = destroy.invokeInterfaceMethod(
                    MethodDescriptors.SUPPLIER_GET, declaringProviderSupplierHandle);
            ResultHandle ctxHandle = destroy.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class), destroy.loadNull());
            ResultHandle declaringProviderInstanceHandle;
            if (isStatic) {
                // for static disposers, we don't need to resolve this handle
                // the `null` will only be used for reflective invocation in case the disposer is private, which is OK
                declaringProviderInstanceHandle = destroy.loadNull();
            } else {
                declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(
                        MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, declaringProviderHandle,
                        ctxHandle);
                if (bean.getDeclaringBean().getScope().isNormal()) {
                    // We need to unwrap the client proxy
                    declaringProviderInstanceHandle = destroy.invokeInterfaceMethod(
                            MethodDescriptors.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                            declaringProviderInstanceHandle);
                }
            }

            ResultHandle[] referenceHandles = new ResultHandle[disposerMethod.parametersCount()];
            int disposedParamPosition = bean.getDisposer().getDisposedParameter().position();
            Iterator<InjectionPointInfo> injectionPointsIterator = bean.getDisposer().getInjection().injectionPoints.iterator();
            for (int i = 0; i < disposerMethod.parametersCount(); i++) {
                if (i == disposedParamPosition) {
                    referenceHandles[i] = destroy.getMethodParam(0);
                } else {
                    InjectionPointInfo injectionPoint = injectionPointsIterator.next();
                    ResultHandle childCtxHandle = destroy.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                            declaringProviderHandle, ctxHandle);
                    ResultHandle providerSupplierHandle = destroy
                            .readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                                    injectionPointToProviderField.get(injectionPoint),
                                    Supplier.class.getName()), destroy.getThis());
                    ResultHandle providerHandle = destroy.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                            providerSupplierHandle);
                    AssignableResultHandle referenceHandle = destroy.createVariable(Object.class);
                    destroy.assign(referenceHandle, destroy.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                            providerHandle, childCtxHandle));
                    checkPrimitiveInjection(destroy, injectionPoint, referenceHandle);
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
                            destroy.loadClass(disposerMethod.parameterType(i).name().toString()));
                    destroy.writeArrayValue(argsArray, i, referenceHandles[i]);
                }
                reflectionRegistration.registerMethod(disposerMethod);
                destroy.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                        destroy.loadClass(disposerMethod.declaringClass().name().toString()),
                        destroy.load(disposerMethod.name()), paramTypesArray, declaringProviderInstanceHandle, argsArray);
            } else if (isStatic) {
                destroy.invokeStaticMethod(MethodDescriptor.of(disposerMethod), referenceHandles);
            } else {
                destroy.invokeVirtualMethod(MethodDescriptor.of(disposerMethod), declaringProviderInstanceHandle,
                        referenceHandles);
            }

            // Destroy @Dependent instances injected into method parameters of a disposer method
            destroy.invokeInterfaceMethod(MethodDescriptors.CREATIONAL_CTX_RELEASE, ctxHandle);

            // If the declaring bean is @Dependent and the disposer is not static, we must destroy the instance afterwards
            if (BuiltinScope.DEPENDENT.is(bean.getDisposer().getDeclaringBean().getScope()) && !isStatic) {
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
                .setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeDestroy.returnValue(bridgeDestroy.invokeVirtualMethod(destroy.getMethodDescriptor(), bridgeDestroy.getThis(),
                bridgeDestroy.getMethodParam(0),
                bridgeDestroy.getMethodParam(1)));
    }

    protected void implementCreate(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean, ProviderType providerType,
            String baseName, Map<InjectionPointInfo, String> injectionPointToProviderSupplierField,
            Map<InterceptorInfo, String> interceptorToProviderSupplierField,
            Map<DecoratorInfo, String> decoratorToProviderSupplierField,
            String targetPackage, boolean isApplicationClass) {

        MethodCreator doCreate = beanCreator
                .getMethodCreator("doCreate", providerType.descriptorName(), CreationalContext.class)
                .setModifiers(ACC_PRIVATE);

        if (bean.isClassBean()) {
            implementCreateForClassBean(classOutput, beanCreator, bean, providerType, baseName,
                    injectionPointToProviderSupplierField, interceptorToProviderSupplierField, decoratorToProviderSupplierField,
                    reflectionRegistration,
                    targetPackage, isApplicationClass, doCreate);
        } else if (bean.isProducerMethod()) {
            implementCreateForProducerMethod(classOutput, beanCreator, bean, providerType, baseName,
                    injectionPointToProviderSupplierField, reflectionRegistration,
                    targetPackage, isApplicationClass, doCreate);
        } else if (bean.isProducerField()) {
            implementCreateForProducerField(classOutput, beanCreator, bean, providerType, baseName,
                    injectionPointToProviderSupplierField, reflectionRegistration,
                    targetPackage, isApplicationClass, doCreate);
        } else if (bean.isSynthetic()) {
            implementCreateForSyntheticBean(beanCreator, bean, providerType, injectionPointToProviderSupplierField, doCreate);
        }

        MethodCreator create = beanCreator.getMethodCreator("create", providerType.descriptorName(), CreationalContext.class)
                .setModifiers(ACC_PUBLIC);
        TryBlock tryBlock = create.tryBlock();
        tryBlock.returnValue(
                tryBlock.invokeSpecialMethod(doCreate.getMethodDescriptor(), tryBlock.getThis(), tryBlock.getMethodParam(0)));
        // `Reflections.newInstance()` throws `CreationException` on its own,
        // but that's handled like all other `RuntimeException`s
        // also ignore custom Throwables, they are virtually never used in practice
        CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
        catchBlock.ifFalse(catchBlock.instanceOf(catchBlock.getCaughtException(), RuntimeException.class))
                .falseBranch().throwException(catchBlock.getCaughtException());
        ResultHandle creationException = catchBlock.newInstance(
                MethodDescriptor.ofConstructor(CreationException.class, Throwable.class),
                catchBlock.getCaughtException());
        catchBlock.throwException(creationException);

        // Bridge method needed
        MethodCreator bridgeCreate = beanCreator.getMethodCreator("create", Object.class, CreationalContext.class)
                .setModifiers(ACC_PUBLIC | ACC_BRIDGE);
        bridgeCreate.returnValue(bridgeCreate.invokeVirtualMethod(create.getMethodDescriptor(), bridgeCreate.getThis(),
                bridgeCreate.getMethodParam(0)));
    }

    private void implementCreateForSyntheticBean(ClassCreator beanCreator, BeanInfo bean, ProviderType providerType,
            Map<InjectionPointInfo, String> injectionPointToProviderSupplierField, MethodCreator doCreate) {

        MethodCreator createSynthetic = beanCreator
                .getMethodCreator("createSynthetic", providerType.descriptorName(), SyntheticCreationalContext.class)
                .setModifiers(ACC_PRIVATE);
        bean.getCreatorConsumer().accept(createSynthetic);

        ResultHandle injectedReferences;
        if (injectionPointToProviderSupplierField.isEmpty()) {
            injectedReferences = doCreate.invokeStaticMethod(MethodDescriptors.COLLECTIONS_EMPTY_MAP);
        } else {
            // Initialize injected references
            injectedReferences = doCreate.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            ResultHandle tccl = doCreate.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL,
                    doCreate.invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD));
            for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
                TryBlock tryBlock = doCreate.tryBlock();
                ResultHandle requiredType;
                try {
                    requiredType = Types.getTypeHandle(tryBlock, injectionPoint.getType(), tccl);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Unable to construct the type handle for " + injectionPoint.getType() + ": " + e.getMessage());
                }
                ResultHandle qualifiersArray;
                if (injectionPoint.hasDefaultedQualifier()) {
                    qualifiersArray = tryBlock.loadNull();
                } else {
                    qualifiersArray = tryBlock.newArray(Annotation.class, injectionPoint.getRequiredQualifiers().size());
                    int qualifierIndex = 0;
                    for (AnnotationInstance qualifierAnnotation : injectionPoint.getRequiredQualifiers()) {
                        BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                        if (qualifier != null) {
                            tryBlock.writeArrayValue(qualifiersArray, tryBlock.load(qualifierIndex++),
                                    qualifier.getLiteralInstance(tryBlock));
                        } else {
                            // Create the annotation literal first
                            ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifierAnnotation.name());
                            tryBlock.writeArrayValue(qualifiersArray, tryBlock.load(qualifierIndex++),
                                    annotationLiterals.create(tryBlock, qualifierClass, qualifierAnnotation));
                        }
                    }
                }
                ResultHandle typeAndQualifiers = tryBlock.newInstance(
                        MethodDescriptor.ofConstructor(TypeAndQualifiers.class, java.lang.reflect.Type.class,
                                Annotation[].class),
                        requiredType, qualifiersArray);

                ResultHandle providerSupplierHandle = tryBlock.readInstanceField(
                        FieldDescriptor.of(beanCreator.getClassName(),
                                injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                        tryBlock.getThis());
                ResultHandle providerHandle = tryBlock.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, providerSupplierHandle);
                ResultHandle childCtxHandle = tryBlock.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                        providerHandle, tryBlock.getMethodParam(0));
                AssignableResultHandle injectedReference = tryBlock.createVariable(Object.class);
                tryBlock.assign(injectedReference, tryBlock.invokeInterfaceMethod(
                        MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle));
                checkPrimitiveInjection(tryBlock, injectionPoint, injectedReference);
                tryBlock.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, injectedReferences, typeAndQualifiers,
                        injectedReference);

                CatchBlockCreator catchBlock = tryBlock.addCatch(RuntimeException.class);
                catchBlock.throwException(RuntimeException.class,
                        "Error injecting synthetic injection point of bean: " + bean.getIdentifier(),
                        catchBlock.getCaughtException());
            }
        }
        ResultHandle paramsHandle = doCreate.readInstanceField(
                FieldDescriptor.of(doCreate.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                doCreate.getThis());
        ResultHandle syntheticCreationalContext = doCreate.newInstance(
                MethodDescriptor.ofConstructor(SyntheticCreationalContextImpl.class, CreationalContext.class, Map.class,
                        Map.class),
                doCreate.getMethodParam(0), paramsHandle, injectedReferences);

        AssignableResultHandle ret = doCreate.createVariable(providerType.descriptorName());
        TryBlock tryBlock = doCreate.tryBlock();
        tryBlock.assign(ret, tryBlock.invokeVirtualMethod(createSynthetic.getMethodDescriptor(), tryBlock.getThis(),
                syntheticCreationalContext));
        CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
        StringBuilderGenerator strBuilder = Gizmo.newStringBuilder(catchBlock);
        strBuilder.append("Error creating synthetic bean [");
        strBuilder.append(bean.getIdentifier());
        strBuilder.append("]: ");
        strBuilder.append(Gizmo.toString(catchBlock, catchBlock.getCaughtException()));
        ResultHandle exception = catchBlock.newInstance(
                MethodDescriptor.ofConstructor(CreationException.class, String.class, Throwable.class),
                strBuilder.callToString(), catchBlock.getCaughtException());
        catchBlock.throwException(exception);

        if (bean.getScope().isNormal()) {
            // Normal scoped synthetic beans should never return null
            BytecodeCreator nullBeanInstance = doCreate.ifNull(ret).trueBranch();
            StringBuilderGenerator message = Gizmo.newStringBuilder(nullBeanInstance);
            message.append("Null contextual instance was produced by a normal scoped synthetic bean: ");
            message.append(Gizmo.toString(nullBeanInstance, nullBeanInstance.getThis()));
            ResultHandle e = nullBeanInstance.newInstance(
                    MethodDescriptor.ofConstructor(CreationException.class, String.class), message.callToString());
            nullBeanInstance.throwException(e);
        }
        doCreate.returnValue(ret);
    }

    private void newProviderHandles(BeanInfo bean, ClassCreator beanCreator, MethodCreator createMethod,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            Map<InterceptorInfo, String> interceptorToProviderField,
            Map<DecoratorInfo, String> decoratorToProviderSupplierField,
            Map<InterceptorInfo, ResultHandle> interceptorToWrap,
            List<TransientReference> transientReferences,
            List<ResultHandle> injectableParamHandles,
            List<ResultHandle> allOtherParamHandles) {

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
                AssignableResultHandle referenceHandle = createMethod.createVariable(Object.class);
                createMethod.assign(referenceHandle, createMethod
                        .invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET, providerHandle, childCtx));
                checkPrimitiveInjection(createMethod, injectionPoint, referenceHandle);
                injectableParamHandles.add(referenceHandle);
                if (injectionPoint.isDependentTransientReference()) {
                    transientReferences.add(new TransientReference(providerHandle, referenceHandle, childCtx));
                }
            }
        }
        if (bean.isSubclassRequired()) {
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                ResultHandle wrapped = interceptorToWrap.get(interceptor);
                if (wrapped != null) {
                    allOtherParamHandles.add(wrapped);
                } else {
                    ResultHandle interceptorProviderSupplierHandle = createMethod.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(), interceptorToProviderField.get(interceptor),
                                    Supplier.class),
                            createMethod.getThis());
                    ResultHandle interceptorProviderHandle = createMethod.invokeInterfaceMethod(
                            MethodDescriptors.SUPPLIER_GET, interceptorProviderSupplierHandle);
                    allOtherParamHandles.add(interceptorProviderHandle);
                }
            }
            for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                ResultHandle decoratorProviderSupplierHandle = createMethod.readInstanceField(
                        FieldDescriptor.of(beanCreator.getClassName(), decoratorToProviderSupplierField.get(decorator),
                                Supplier.class),
                        createMethod.getThis());
                ResultHandle decoratorProviderHandle = createMethod.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, decoratorProviderSupplierHandle);
                allOtherParamHandles.add(decoratorProviderHandle);
            }
        }
    }

    static void checkPrimitiveInjection(BytecodeCreator bytecode, InjectionPointInfo injectionPoint,
            AssignableResultHandle referenceHandle) {
        if (injectionPoint.getType().kind() == Type.Kind.PRIMITIVE) {
            Type type = null;
            if (injectionPoint.getResolvedBean().isProducerField()) {
                type = injectionPoint.getResolvedBean().getTarget().get().asField().type();
            } else if (injectionPoint.getResolvedBean().isProducerMethod()) {
                type = injectionPoint.getResolvedBean().getTarget().get().asMethod().returnType();
            }

            if (type != null && Types.isPrimitiveWrapperType(type)) {
                BytecodeCreator isNull = bytecode.ifNull(referenceHandle).trueBranch();
                isNull.assign(referenceHandle,
                        Types.loadPrimitiveDefault(injectionPoint.getType().asPrimitiveType().primitive(), isNull));
            }
        }
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
            List<DecoratorInfo> decorators = bean.getBoundDecorators();
            List<String> paramTypes = new ArrayList<>();
            List<ResultHandle> paramHandles = new ArrayList<>();

            // 1. constructor injection points
            for (int i = 0; i < injectionPoints.size(); i++) {
                paramTypes.add(injectionPoints.get(i).getType().name().toString());
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
            // 4. decorators
            for (int i = 0; i < decorators.size(); i++) {
                paramTypes.add(InjectableDecorator.class.getName());
                paramHandles.add(providerHandles.get(injectionPoints.size() + interceptors.size() + i));
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
                            creator.loadClass(injectionPoints.get(i).getType().name().toString()));
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
                    paramTypes[iterator.previousIndex()] = DescriptorUtils.typeToString(injectionPoint.getType());
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
                        creator.loadClass(noArgsConstructor.declaringClass().name().toString()), paramTypesArray,
                        argsArray);
            } else {
                // new SimpleBean()
                return creator.newInstance(MethodDescriptor.ofConstructor(providerTypeName));
            }
        }
    }

    void implementCreateForProducerField(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            ProviderType providerType, String baseName, Map<InjectionPointInfo, String> injectionPointToProviderSupplierField,
            ReflectionRegistration reflectionRegistration,
            String targetPackage, boolean isApplicationClass, MethodCreator create) {

        AssignableResultHandle instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerType.className()));
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
                    create.loadClass(producerField.declaringClass().name().toString()),
                    create.load(producerField.name()),
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
    }

    void implementCreateForProducerMethod(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            ProviderType providerType, String baseName, Map<InjectionPointInfo, String> injectionPointToProviderSupplierField,
            ReflectionRegistration reflectionRegistration,
            String targetPackage, boolean isApplicationClass, MethodCreator create) {

        AssignableResultHandle instanceHandle;

        MethodInfo producerMethod = bean.getTarget().get().asMethod();
        boolean isStatic = Modifier.isStatic(producerMethod.flags());

        instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerType.className()));
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
        if (isStatic) {
            // for static producers, we don't need to resolve this handle
            // the `null` will only be used for reflective invocation in case the producer is private, which is OK
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
        List<TransientReference> transientReferences = new ArrayList<>();
        int paramIdx = 0;
        for (InjectionPointInfo injectionPoint : injectionPoints) {
            ResultHandle providerSupplierHandle = create.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                    injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                    create.getThis());
            ResultHandle providerHandle = create.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                    providerSupplierHandle);
            ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                    providerHandle, create.getMethodParam(0));
            AssignableResultHandle referenceHandle = create.createVariable(Object.class);
            create.assign(referenceHandle, create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                    providerHandle, childCtxHandle));
            checkPrimitiveInjection(create, injectionPoint, referenceHandle);
            referenceHandles[paramIdx++] = referenceHandle;
            // We need to destroy dependent beans for @TransientReference injection points
            if (injectionPoint.isDependentTransientReference()) {
                transientReferences.add(new TransientReference(providerHandle, referenceHandle, childCtxHandle));
            }
        }

        if (Modifier.isPrivate(producerMethod.flags())) {
            privateMembers.add(isApplicationClass, String.format("Producer method %s#%s()",
                    producerMethod.declaringClass().name(), producerMethod.name()));
            ResultHandle paramTypesArray = create.newArray(Class.class, create.load(referenceHandles.length));
            ResultHandle argsArray = create.newArray(Object.class, create.load(referenceHandles.length));
            for (int i = 0; i < referenceHandles.length; i++) {
                create.writeArrayValue(paramTypesArray, i,
                        create.loadClass(producerMethod.parameterType(i).name().toString()));
                create.writeArrayValue(argsArray, i, referenceHandles[i]);
            }
            reflectionRegistration.registerMethod(producerMethod);
            create.assign(instanceHandle, create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                    create.loadClass(producerMethod.declaringClass().name().toString()),
                    create.load(producerMethod.name()),
                    paramTypesArray,
                    declaringProviderInstanceHandle,
                    argsArray));
        } else {
            ResultHandle invokeMethodHandle;
            if (isStatic) {
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

        // If the declaring bean is @Dependent and the producer is not static, we must destroy the instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope()) && !isStatic) {
            create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_BEAN_DESTROY, declaringProviderHandle,
                    declaringProviderInstanceHandle, ctxHandle);
        }
        // Destroy injected transient references
        destroyTransientReferences(create, transientReferences);

        create.returnValue(instanceHandle);
    }

    void implementCreateForClassBean(ClassOutput classOutput, ClassCreator beanCreator, BeanInfo bean,
            ProviderType providerType,
            String baseName, Map<InjectionPointInfo, String> injectionPointToProviderSupplierField,
            Map<InterceptorInfo, String> interceptorToProviderSupplierField,
            Map<DecoratorInfo, String> decoratorToProviderSupplierField,
            ReflectionRegistration reflectionRegistration, String targetPackage, boolean isApplicationClass,
            MethodCreator create) {

        AssignableResultHandle instanceHandle;

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
        instanceHandle = create.createVariable(DescriptorUtils.extToInt(providerType.className()));
        if (!aroundConstructs.isEmpty()) {
            Optional<Injection> constructorInjection = bean.getConstructorInjection();
            ResultHandle constructorHandle;
            if (constructorInjection.isPresent()) {
                List<String> paramTypes = new ArrayList<>();
                for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                    paramTypes.add(injectionPoint.getType().name().toString());
                }
                ResultHandle[] paramsHandles = new ResultHandle[2];
                paramsHandles[0] = create.loadClass(providerType.className());
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
                paramsHandles[0] = create.loadClass(providerType.className());
                paramsHandles[1] = create.newArray(Class.class, create.load(0));
                constructorHandle = create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                        paramsHandles);
                MethodInfo noArgsConstructor = bean.getTarget().get().asClass().method(Methods.INIT);
                reflectionRegistration.registerMethod(noArgsConstructor);
            }

            List<TransientReference> transientReferences = new ArrayList<>();
            // List of handles representing injectable parameters
            List<ResultHandle> injectableCtorParams = new ArrayList<>();
            // list of handles representing all other parameters, such as injectable interceptors
            List<ResultHandle> allOtherCtorParams = new ArrayList<>();
            newProviderHandles(bean, beanCreator, create,
                    injectionPointToProviderSupplierField, interceptorToProviderSupplierField, decoratorToProviderSupplierField,
                    interceptorToWrap, transientReferences, injectableCtorParams, allOtherCtorParams);

            // Forwarding function
            // Function<Object[], Object> forward = (params) -> new SimpleBean_Subclass(params[0], ctx, lifecycleInterceptorProvider1)
            FunctionCreator func = create.createFunction(Function.class);
            BytecodeCreator funcBytecode = func.getBytecode();
            List<ResultHandle> params = new ArrayList<>();
            if (!injectableCtorParams.isEmpty()) {
                // `injectableCtorParams` are passed to the first interceptor in the chain
                // the `Function` generated here obtains the parameter array from `InvocationContext`
                // these 2 arrays have the same shape (size and element types), but not necessarily the same content
                ResultHandle paramsArray = funcBytecode.checkCast(funcBytecode.getMethodParam(0), Object[].class);
                for (int i = 0; i < injectableCtorParams.size(); i++) {
                    params.add(funcBytecode.readArrayValue(paramsArray, i));
                }
            }
            List<ResultHandle> providerHandles = new ArrayList<>(params);
            providerHandles.addAll(allOtherCtorParams);
            ResultHandle retHandle = newInstanceHandle(bean, beanCreator, funcBytecode, create, providerType.className(),
                    baseName,
                    providerHandles,
                    reflectionRegistration, isApplicationClass);
            // Destroy injected transient references
            destroyTransientReferences(funcBytecode, transientReferences);
            funcBytecode.returnValue(retHandle);

            // Interceptor bindings
            ResultHandle bindingsArray = create.newArray(Object.class, aroundConstructs.bindings.size());
            int bindingsIndex = 0;
            for (AnnotationInstance binding : aroundConstructs.bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                create.writeArrayValue(bindingsArray, bindingsIndex++,
                        annotationLiterals.create(create, bindingClass, binding));
            }
            // ResultHandle of Object[] holding all constructor args
            ResultHandle ctorArgsArray = create.newArray(Object.class, create.load(injectableCtorParams.size()));
            for (int i = 0; i < injectableCtorParams.size(); i++) {
                create.writeArrayValue(ctorArgsArray, i, injectableCtorParams.get(i));
            }
            ResultHandle invocationContextHandle = create.invokeStaticMethod(
                    MethodDescriptors.INVOCATION_CONTEXTS_AROUND_CONSTRUCT, constructorHandle,
                    ctorArgsArray,
                    aroundConstructsHandle, func.getInstance(),
                    create.invokeStaticMethod(MethodDescriptors.SETS_OF, bindingsArray));
            TryBlock tryCatch = create.tryBlock();
            CatchBlockCreator exceptionCatch = tryCatch.addCatch(Exception.class);
            exceptionCatch.ifFalse(exceptionCatch.instanceOf(exceptionCatch.getCaughtException(), RuntimeException.class))
                    .falseBranch().throwException(exceptionCatch.getCaughtException());
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
            List<TransientReference> transientReferences = new ArrayList<>();
            List<ResultHandle> providerHandles = new ArrayList<>();
            newProviderHandles(bean, beanCreator, create, injectionPointToProviderSupplierField,
                    interceptorToProviderSupplierField, decoratorToProviderSupplierField,
                    interceptorToWrap, transientReferences, providerHandles, providerHandles);
            create.assign(instanceHandle,
                    newInstanceHandle(bean, beanCreator, create, create, providerType.className(), baseName,
                            providerHandles,
                            reflectionRegistration, isApplicationClass));
            // Destroy injected transient references
            destroyTransientReferences(create, transientReferences);
        }

        // Perform field and initializer injections
        for (Injection injection : bean.getInjections()) {
            if (injection.isField()) {
                TryBlock tryBlock = create.tryBlock();
                InjectionPointInfo injectionPoint = injection.injectionPoints.get(0);
                ResultHandle providerSupplierHandle = tryBlock.readInstanceField(FieldDescriptor.of(beanCreator.getClassName(),
                        injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                        tryBlock.getThis());
                ResultHandle providerHandle = tryBlock.invokeInterfaceMethod(
                        MethodDescriptors.SUPPLIER_GET, providerSupplierHandle);
                ResultHandle childCtxHandle = tryBlock.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                        providerHandle, tryBlock.getMethodParam(0));
                AssignableResultHandle referenceHandle = tryBlock.createVariable(Object.class);
                tryBlock.assign(referenceHandle, tryBlock.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                        providerHandle, childCtxHandle));
                checkPrimitiveInjection(tryBlock, injectionPoint, referenceHandle);

                FieldInfo injectedField = injection.target.asField();
                // only use reflection fallback if we are not performing transformation
                if (isReflectionFallbackNeeded(injectedField, targetPackage, bean)) {
                    if (Modifier.isPrivate(injectedField.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@Inject field %s#%s", injection.target.asField().declaringClass().name(),
                                        injection.target.asField().name()));
                    }
                    reflectionRegistration.registerField(injectedField);
                    tryBlock.invokeStaticMethod(MethodDescriptors.REFLECTIONS_WRITE_FIELD,
                            tryBlock.loadClass(injectedField.declaringClass().name().toString()),
                            tryBlock.load(injectedField.name()), instanceHandle, referenceHandle);

                } else {
                    // We cannot use injectionPoint.getRequiredType() because it might be a resolved parameterize type and we could get NoSuchFieldError
                    tryBlock.writeInstanceField(
                            FieldDescriptor.of(injectedField.declaringClass().name().toString(), injectedField.name(),
                                    DescriptorUtils.typeToString(injectionPoint.getTarget().asField().type())),
                            instanceHandle, referenceHandle);
                }
                CatchBlockCreator catchBlock = tryBlock.addCatch(RuntimeException.class);
                catchBlock.throwException(RuntimeException.class, "Error injecting " + injection.target,
                        catchBlock.getCaughtException());
            } else if (injection.isMethod() && !injection.isConstructor()) {
                List<TransientReference> transientReferences = new ArrayList<>();
                ResultHandle[] referenceHandles = new ResultHandle[injection.injectionPoints.size()];
                int paramIdx = 0;
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    ResultHandle providerSupplierHandle = create.readInstanceField(
                            FieldDescriptor.of(beanCreator.getClassName(),
                                    injectionPointToProviderSupplierField.get(injectionPoint), Supplier.class.getName()),
                            create.getThis());
                    ResultHandle providerHandle = create.invokeInterfaceMethod(MethodDescriptors.SUPPLIER_GET,
                            providerSupplierHandle);
                    ResultHandle childCtxHandle = create.invokeStaticMethod(MethodDescriptors.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                            providerHandle, create.getMethodParam(0));
                    AssignableResultHandle referenceHandle = create.createVariable(Object.class);
                    create.assign(referenceHandle, create.invokeInterfaceMethod(MethodDescriptors.INJECTABLE_REF_PROVIDER_GET,
                            providerHandle, childCtxHandle));
                    checkPrimitiveInjection(create, injectionPoint, referenceHandle);
                    referenceHandles[paramIdx++] = referenceHandle;
                    // We need to destroy dependent beans for @TransientReference injection points
                    if (injectionPoint.isDependentTransientReference()) {
                        transientReferences.add(new TransientReference(providerHandle, referenceHandle, childCtxHandle));
                    }
                }

                MethodInfo initializerMethod = injection.target.asMethod();
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
                                create.loadClass(initializerMethod.parameterType(i).name().toString()));
                        create.writeArrayValue(argsArray, i, referenceHandles[i]);
                    }
                    reflectionRegistration.registerMethod(initializerMethod);
                    create.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                            create.loadClass(initializerMethod.declaringClass().name().toString()),
                            create.load(injection.target.asMethod().name()),
                            paramTypesArray, instanceHandle, argsArray);

                } else {
                    create.invokeVirtualMethod(MethodDescriptor.of(injection.target.asMethod()), instanceHandle,
                            referenceHandles);
                }

                // Destroy injected transient references
                destroyTransientReferences(create, transientReferences);
            }
        }

        if (bean.isSubclassRequired()) {
            // marking the *_Subclass instance as constructed not when its constructor finishes,
            // but only after injection is complete, to satisfy the Interceptors specification:
            //
            // > With the exception of `@AroundConstruct` lifecycle callback interceptor methods,
            // > no interceptor methods are invoked until after dependency injection has been completed
            // > on both the interceptor instances and the target.
            //
            // and also the CDI specification:
            //
            // > Invocations of initializer methods by the container are not business method invocations.
            String subclassName = SubclassGenerator.generatedName(bean.getProviderType().name(), baseName);
            create.invokeVirtualMethod(MethodDescriptor.ofMethod(subclassName,
                    SubclassGenerator.MARK_CONSTRUCTED_METHOD_NAME, void.class), instanceHandle);
        }

        // if there's no `@PostConstruct` interceptor, we'll generate code to invoke `@PostConstruct` callbacks
        // directly into the `doCreate` method:
        //
        // private MyBean doCreate(CreationalContext var1) {
        //     MyBean var2 = new MyBean();
        //     var2.myPostConstructCallback();
        //     return var2;
        // }
        BytecodeCreator postConstructsBytecode = create;

        // PostConstruct lifecycle callback interceptors
        InterceptionInfo postConstructs = bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT);
        if (!postConstructs.isEmpty()) {
            // if there _is_ some `@PostConstruct` interceptor, however, we'll reify the chain of `@PostConstruct`
            // callbacks into a `Runnable` that we pass into the interceptor chain to be called
            // by the last `proceed()` call:
            //
            // private MyBean doCreate(CreationalContext var1) {
            //     ...
            //     MyBean var7 = new MyBean();
            //     // this is a `Runnable` that calls `MyBean.myPostConstructCallback()`
            //     MyBean_Bean$$function$$1 var11 = new MyBean_Bean$$function$$1(var7);
            //     ...
            //     InvocationContext var12 = InvocationContexts.postConstruct(var7, (List)var5, var10, (Runnable)var11);
            //     var12.proceed();
            //     return var7;
            // }
            FunctionCreator postConstructForwarder = create.createFunction(Runnable.class);
            postConstructsBytecode = postConstructForwarder.getBytecode();

            // Interceptor bindings
            ResultHandle bindingsArray = create.newArray(Object.class, postConstructs.bindings.size());
            int bindingsIndex = 0;
            for (AnnotationInstance binding : postConstructs.bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                create.writeArrayValue(bindingsArray, bindingsIndex++,
                        annotationLiterals.create(create, bindingClass, binding));

            }

            // InvocationContextImpl.postConstruct(instance,postConstructs).proceed()
            ResultHandle invocationContextHandle = create.invokeStaticMethod(
                    MethodDescriptors.INVOCATION_CONTEXTS_POST_CONSTRUCT, instanceHandle,
                    postConstructsHandle, create.invokeStaticMethod(MethodDescriptors.SETS_OF, bindingsArray),
                    postConstructForwarder.getInstance());

            TryBlock tryCatch = create.tryBlock();
            CatchBlockCreator exceptionCatch = tryCatch.addCatch(Exception.class);
            exceptionCatch.ifFalse(exceptionCatch.instanceOf(exceptionCatch.getCaughtException(), RuntimeException.class))
                    .falseBranch().throwException(exceptionCatch.getCaughtException());
            // throw new RuntimeException(e)
            exceptionCatch.throwException(RuntimeException.class, "Error invoking postConstructs",
                    exceptionCatch.getCaughtException());
            tryCatch.invokeInterfaceMethod(MethodDescriptor.ofMethod(InvocationContext.class, "proceed", Object.class),
                    invocationContextHandle);
        }

        // PostConstruct callbacks
        // possibly wrapped into Runnable so that PostConstruct interceptors can proceed() correctly
        if (!bean.isInterceptor()) {
            List<MethodInfo> postConstructCallbacks = Beans.getCallbacks(bean.getTarget().get().asClass(),
                    DotNames.POST_CONSTRUCT, bean.getDeployment().getBeanArchiveIndex());

            for (MethodInfo callback : postConstructCallbacks) {
                if (isReflectionFallbackNeeded(callback, targetPackage)) {
                    if (Modifier.isPrivate(callback.flags())) {
                        privateMembers.add(isApplicationClass,
                                String.format("@PostConstruct callback %s#%s()", callback.declaringClass().name(),
                                        callback.name()));
                    }
                    reflectionRegistration.registerMethod(callback);
                    postConstructsBytecode.invokeStaticMethod(MethodDescriptors.REFLECTIONS_INVOKE_METHOD,
                            postConstructsBytecode.loadClass(callback.declaringClass().name().toString()),
                            postConstructsBytecode.load(callback.name()),
                            postConstructsBytecode.newArray(Class.class, postConstructsBytecode.load(0)), instanceHandle,
                            postConstructsBytecode.newArray(Object.class, postConstructsBytecode.load(0)));
                } else {
                    postConstructsBytecode.invokeVirtualMethod(MethodDescriptor.of(callback), instanceHandle);
                }
            }
        }
        if (postConstructsBytecode != create) {
            // only if we're generating a `Runnable`, see above
            postConstructsBytecode.returnVoid();
        }

        create.returnValue(instanceHandle);
    }

    protected void implementGet(BeanInfo bean, ClassCreator beanCreator, ProviderType providerType, String baseName) {

        MethodCreator get = beanCreator.getMethodCreator("get", providerType.descriptorName(), CreationalContext.class)
                .setModifiers(ACC_PUBLIC);

        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            // @Dependent pseudo-scope
            // Foo instance = create(ctx)
            ResultHandle instance = get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), "create", providerType.descriptorName(),
                            CreationalContext.class),
                    get.getThis(),
                    get.getMethodParam(0));

            // We can optimize if:
            // 1) class bean - has no @PreDestroy interceptor and there is no @PreDestroy callback
            // 2) producer - there is no disposal method
            // 3) synthetic bean - has no destruction logic
            if (!bean.hasDestroyLogic()) {
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
        } else if (bean.getScope().isNormal()) {
            // All normal scopes
            // return proxy()
            get.returnValue(get.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(beanCreator.getClassName(), FIELD_NAME_PROXY, getProxyTypeName(bean, baseName)),
                    get.getThis()));
        } else {
            // All pseudo scopes other than @Dependent (incl. @Singleton)
            // return Arc.container().getActiveContext(getScope()).get(this, new CreationalContextImpl<>())
            ResultHandle container = get.invokeStaticMethod(MethodDescriptors.ARC_CONTAINER);
            ResultHandle creationalContext = get.newInstance(
                    MethodDescriptor.ofConstructor(CreationalContextImpl.class, Contextual.class),
                    get.getThis());
            ResultHandle scope = get.loadClass(bean.getScope().getDotName().toString());
            ResultHandle context = get.invokeInterfaceMethod(MethodDescriptors.ARC_CONTAINER_GET_ACTIVE_CONTEXT, container,
                    scope);
            get.returnValue(
                    get.invokeInterfaceMethod(MethodDescriptors.CONTEXT_GET, context, get.getThis(), creationalContext));
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

    protected void implementEquals(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator equals = beanCreator.getMethodCreator("equals", boolean.class, Object.class).setModifiers(ACC_PUBLIC);
        final ResultHandle obj = equals.getMethodParam(0);
        // if (this == obj) {
        //    return true;
        // }
        equals.ifReferencesEqual(equals.getThis(), obj)
                .trueBranch().returnValue(equals.load(true));

        // if (obj == null) {
        //    return false;
        // }
        equals.ifNull(obj).trueBranch().returnValue(equals.load(false));
        // if (!(obj instanceof InjectableBean)) {
        //    return false;
        // }
        equals.ifFalse(equals.instanceOf(obj, InjectableBean.class)).trueBranch()
                .returnValue(equals.load(false));
        // return identifier.equals(((InjectableBean) obj).getIdentifier());
        ResultHandle injectableBean = equals.checkCast(obj, InjectableBean.class);
        ResultHandle otherIdentifier = equals.invokeInterfaceMethod(MethodDescriptors.GET_IDENTIFIER, injectableBean);
        equals.returnValue(equals.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS, equals.load(bean.getIdentifier()),
                otherIdentifier));
    }

    protected void implementHashCode(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator hashCode = beanCreator.getMethodCreator("hashCode", int.class).setModifiers(ACC_PUBLIC);
        final ResultHandle constantHashCodeResult = hashCode.load(bean.getIdentifier().hashCode());
        hashCode.returnValue(constantHashCodeResult);
    }

    protected void implementToString(ClassCreator beanCreator) {
        MethodCreator toString = beanCreator.getMethodCreator("toString", String.class).setModifiers(ACC_PUBLIC);
        toString.returnValue(toString.invokeStaticMethod(MethodDescriptors.BEANS_TO_STRING, toString.getThis()));
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

    protected void implementIsAlternative(BeanInfo bean, ClassCreator beanCreator) {
        if (bean.isAlternative()) {
            MethodCreator isAlternative = beanCreator.getMethodCreator("isAlternative", boolean.class)
                    .setModifiers(ACC_PUBLIC);
            isAlternative
                    .returnValue(isAlternative.load(true));
        }
    }

    protected void implementGetPriority(BeanInfo bean, ClassCreator beanCreator) {
        if (bean.getPriority() != null) {
            MethodCreator getPriority = beanCreator.getMethodCreator("getPriority", int.class)
                    .setModifiers(ACC_PUBLIC);
            getPriority
                    .returnValue(getPriority.load(bean.getPriority()));
        }
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

    protected void implementGetImplementationClass(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator getImplementationClass = beanCreator.getMethodCreator("getImplementationClass", Class.class)
                .setModifiers(ACC_PUBLIC);
        getImplementationClass.returnValue(bean.getImplClazz() != null ? getImplementationClass.loadClass(bean.getImplClazz())
                : getImplementationClass.loadNull());
    }

    protected void implementGetName(BeanInfo bean, ClassCreator beanCreator) {
        if (bean.getName() != null) {
            MethodCreator getName = beanCreator.getMethodCreator("getName", String.class)
                    .setModifiers(ACC_PUBLIC);
            getName.returnValue(getName.load(bean.getName()));
        }
    }

    protected void implementGetKind(ClassCreator beanCreator, InjectableBean.Kind kind) {
        MethodCreator getScope = beanCreator.getMethodCreator("getKind", InjectableBean.Kind.class).setModifiers(ACC_PUBLIC);
        getScope.returnValue(getScope
                .readStaticField(FieldDescriptor.of(InjectableBean.Kind.class, kind.toString(), InjectableBean.Kind.class)));
    }

    protected void implementSupplierGet(ClassCreator beanCreator) {
        MethodCreator get = beanCreator.getMethodCreator("get", Object.class).setModifiers(ACC_PUBLIC);
        get.returnValue(get.getThis());
    }

    protected void implementIsSuppressed(BeanInfo bean, ClassCreator beanCreator) {
        MethodCreator isSuppressed = beanCreator.getMethodCreator("isSuppressed", boolean.class).setModifiers(ACC_PUBLIC);
        for (Function<BeanInfo, Consumer<BytecodeCreator>> generator : suppressConditionGenerators) {
            Consumer<BytecodeCreator> condition = generator.apply(bean);
            if (condition != null) {
                condition.accept(isSuppressed);
            }
        }
        isSuppressed.returnValue(isSuppressed.load(false));
    }

    private void implementGetInjectionPoints(BeanInfo bean, ClassCreator beanCreator) {
        // this is practically never used at runtime, but it makes the `Bean` classes bigger;
        // let's only implement `getInjectionPoints()` in strict mode, to be able to pass the TCK
        if (!bean.getDeployment().strictCompatibility) {
            return;
        }

        List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
        if (injectionPoints.isEmpty()) {
            // inherit the default implementation from `InjectableBean`
            return;
        }

        MethodCreator getInjectionPoints = beanCreator.getMethodCreator("getInjectionPoints", Set.class);

        ResultHandle tccl = getInjectionPoints.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL,
                getInjectionPoints.invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD));

        ResultHandle result = getInjectionPoints.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        for (InjectionPointInfo injectionPoint : injectionPoints) {
            ResultHandle type = Types.getTypeHandle(getInjectionPoints, injectionPoint.getType(), tccl);
            ResultHandle qualifiers = collectInjectionPointQualifiers(bean.getDeployment(), getInjectionPoints,
                    injectionPoint, annotationLiterals);
            ResultHandle annotations = collectInjectionPointAnnotations(bean.getDeployment(), getInjectionPoints,
                    injectionPoint, annotationLiterals, injectionPointAnnotationsPredicate);
            ResultHandle member = getJavaMemberHandle(getInjectionPoints, injectionPoint, reflectionRegistration);

            ResultHandle ip = getInjectionPoints.newInstance(MethodDescriptors.INJECTION_POINT_IMPL_CONSTRUCTOR,
                    type, type, qualifiers, getInjectionPoints.getThis(), annotations, member,
                    getInjectionPoints.load(injectionPoint.getPosition()),
                    getInjectionPoints.load(injectionPoint.isTransient()));
            getInjectionPoints.invokeInterfaceMethod(MethodDescriptors.SET_ADD, result, ip);

        }
        getInjectionPoints.returnValue(result);
    }

    private String getProxyTypeName(BeanInfo bean, String baseName) {
        StringBuilder proxyTypeName = new StringBuilder();
        proxyTypeName.append(bean.getClientProxyPackageName());
        if (proxyTypeName.length() > 0) {
            proxyTypeName.append(".");
        }
        proxyTypeName.append(baseName);
        proxyTypeName.append(ClientProxyGenerator.CLIENT_PROXY_SUFFIX);
        return proxyTypeName.toString();
    }

    private ResultHandle wrapCurrentInjectionPoint(BeanInfo bean,
            MethodCreator constructor, InjectionPointInfo injectionPoint, int paramIdx, ResultHandle tccl,
            ReflectionRegistration reflectionRegistration) {
        ResultHandle requiredQualifiersHandle = collectInjectionPointQualifiers(bean.getDeployment(),
                constructor, injectionPoint, annotationLiterals);
        ResultHandle annotationsHandle = collectInjectionPointAnnotations(bean.getDeployment(),
                constructor, injectionPoint, annotationLiterals, injectionPointAnnotationsPredicate);
        ResultHandle javaMemberHandle = getJavaMemberHandle(constructor, injectionPoint, reflectionRegistration);

        // TODO empty IP for synthetic injections

        return constructor.newInstance(
                MethodDescriptor.ofConstructor(CurrentInjectionPointProvider.class, InjectableBean.class,
                        Supplier.class, java.lang.reflect.Type.class,
                        Set.class, Set.class, Member.class, int.class, boolean.class),
                constructor.getThis(), constructor.getMethodParam(paramIdx),
                Types.getTypeHandle(constructor, injectionPoint.getType(), tccl),
                requiredQualifiersHandle, annotationsHandle, javaMemberHandle,
                constructor.load(injectionPoint.getPosition()),
                constructor.load(injectionPoint.isTransient()));
    }

    private void initializeProxy(BeanInfo bean, String baseName, ClassCreator beanCreator) {
        // Add proxy volatile field
        String proxyTypeName = getProxyTypeName(bean, baseName);
        beanCreator.getFieldCreator(FIELD_NAME_PROXY, proxyTypeName)
                .setModifiers(ACC_PRIVATE | ACC_VOLATILE);

        // Add proxy() method
        MethodCreator proxy = beanCreator.getMethodCreator(FIELD_NAME_PROXY, proxyTypeName).setModifiers(ACC_PRIVATE);
        AssignableResultHandle proxyInstance = proxy.createVariable(DescriptorUtils.extToInt(proxyTypeName));
        proxy.assign(proxyInstance, proxy.readInstanceField(
                FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_PROXY, proxyTypeName),
                proxy.getThis()));
        // Create a new proxy instance, atomicity does not really matter here
        BytecodeCreator proxyNull = proxy.ifNull(proxyInstance).trueBranch();
        proxyNull.assign(proxyInstance, proxyNull.newInstance(
                MethodDescriptor.ofConstructor(proxyTypeName, String.class), proxyNull.load(bean.getIdentifier())));
        proxyNull.writeInstanceField(FieldDescriptor.of(beanCreator.getClassName(), FIELD_NAME_PROXY, proxyTypeName),
                proxyNull.getThis(),
                proxyInstance);
        proxy.returnValue(proxyInstance);
    }

    public static ResultHandle getJavaMemberHandle(MethodCreator bytecode, InjectionPointInfo injectionPoint,
            ReflectionRegistration reflectionRegistration) {
        ResultHandle javaMemberHandle;
        if (injectionPoint.isSynthetic()) {
            javaMemberHandle = bytecode.loadNull();
        } else if (injectionPoint.isField()) {
            FieldInfo field = injectionPoint.getTarget().asField();
            javaMemberHandle = bytecode.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_FIELD,
                    bytecode.loadClass(field.declaringClass().name().toString()),
                    bytecode.load(field.name()));
            reflectionRegistration.registerField(field);
        } else {
            MethodInfo method = injectionPoint.getTarget().asMethod();
            reflectionRegistration.registerMethod(method);
            if (method.name().equals(Methods.INIT)) {
                // Reflections.findConstructor(org.foo.SimpleBean.class,java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[2];
                paramsHandles[0] = bytecode.loadClass(method.declaringClass().name().toString());
                ResultHandle paramsArray = bytecode.newArray(Class.class, bytecode.load(method.parametersCount()));
                for (ListIterator<Type> iterator = method.parameterTypes().listIterator(); iterator.hasNext();) {
                    bytecode.writeArrayValue(paramsArray, iterator.nextIndex(),
                            bytecode.loadClass(iterator.next().name().toString()));
                }
                paramsHandles[1] = paramsArray;
                javaMemberHandle = bytecode.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_CONSTRUCTOR,
                        paramsHandles);
            } else {
                // Reflections.findMethod(org.foo.SimpleBean.class,"foo",java.lang.String.class)
                ResultHandle[] paramsHandles = new ResultHandle[3];
                paramsHandles[0] = bytecode.loadClass(method.declaringClass().name().toString());
                paramsHandles[1] = bytecode.load(method.name());
                ResultHandle paramsArray = bytecode.newArray(Class.class, bytecode.load(method.parametersCount()));
                for (ListIterator<Type> iterator = method.parameterTypes().listIterator(); iterator.hasNext();) {
                    bytecode.writeArrayValue(paramsArray, iterator.nextIndex(),
                            bytecode.loadClass(iterator.next().name().toString()));
                }
                paramsHandles[2] = paramsArray;
                javaMemberHandle = bytecode.invokeStaticMethod(MethodDescriptors.REFLECTIONS_FIND_METHOD, paramsHandles);
            }
        }
        return javaMemberHandle;
    }

    public static ResultHandle collectInjectionPointAnnotations(BeanDeployment beanDeployment, MethodCreator bytecode,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        if (injectionPoint.isSynthetic()) {
            return bytecode.invokeStaticMethod(MethodDescriptors.COLLECTIONS_EMPTY_SET);
        }
        ResultHandle annotationsHandle = bytecode.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
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
            if (!injectionPointAnnotationsPredicate.test(annotation.name())) {
                continue;
            }
            ResultHandle annotationHandle;
            if (DotNames.INJECT.equals(annotation.name())) {
                annotationHandle = bytecode
                        .readStaticField(FieldDescriptor.of(InjectLiteral.class, "INSTANCE", InjectLiteral.class));
            } else {
                ClassInfo annotationClass = getClassByName(beanDeployment.getBeanArchiveIndex(), annotation.name());
                if (annotationClass == null) {
                    continue;
                }
                annotationHandle = annotationLiterals.create(bytecode, annotationClass, annotation);
            }
            bytecode.invokeInterfaceMethod(MethodDescriptors.SET_ADD, annotationsHandle,
                    annotationHandle);
        }
        return annotationsHandle;
    }

    public static ResultHandle collectInjectionPointQualifiers(BeanDeployment beanDeployment, MethodCreator bytecode,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals) {
        return collectQualifiers(beanDeployment, bytecode, annotationLiterals,
                injectionPoint.hasDefaultedQualifier() ? Collections.emptySet() : injectionPoint.getRequiredQualifiers());
    }

    public static ResultHandle collectQualifiers(BeanDeployment beanDeployment, MethodCreator bytecode,
            AnnotationLiteralProcessor annotationLiterals, Set<AnnotationInstance> qualifiers) {
        ResultHandle qualifiersHandle;
        if (qualifiers.isEmpty()) {
            qualifiersHandle = bytecode.readStaticField(FieldDescriptors.QUALIFIERS_IP_QUALIFIERS);
        } else {
            qualifiersHandle = bytecode.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance qualifierAnnotation : qualifiers) {
                BuiltinQualifier qualifier = BuiltinQualifier.of(qualifierAnnotation);
                ResultHandle qualifierHandle;
                if (qualifier != null) {
                    qualifierHandle = qualifier.getLiteralInstance(bytecode);
                } else {
                    // Create annotation literal if needed
                    qualifierHandle = annotationLiterals.create(bytecode,
                            beanDeployment.getQualifier(qualifierAnnotation.name()), qualifierAnnotation);
                }
                bytecode.invokeInterfaceMethod(MethodDescriptors.SET_ADD, qualifiersHandle,
                        qualifierHandle);
            }
        }
        return qualifiersHandle;
    }

    static void destroyTransientReferences(BytecodeCreator bytecode, Iterable<TransientReference> transientReferences) {
        for (TransientReference transientReference : transientReferences) {
            bytecode.invokeStaticMethod(MethodDescriptors.INJECTABLE_REFERENCE_PROVIDERS_DESTROY, transientReference.provider,
                    transientReference.instance, transientReference.creationalContext);
        }
    }

    static class TransientReference {

        final ResultHandle provider;
        final ResultHandle instance;
        final ResultHandle creationalContext;

        public TransientReference(ResultHandle provider, ResultHandle contextualInstance, ResultHandle creationalContext) {
            this.provider = provider;
            this.instance = contextualInstance;
            this.creationalContext = creationalContext;
        }

    }

    /**
     *
     * @see InjectableReferenceProvider
     */
    static final class ProviderType {

        private final Type type;

        public ProviderType(Type type) {
            this.type = type;
        }

        DotName name() {
            return type.name();
        }

        /**
         *
         * @return the class name, e.g. {@code org.acme.Foo}
         */
        String className() {
            return type.name().toString();
        }

        /**
         *
         * @return the name used in JVM descriptors, e.g. {@code Lorg/acme/Foo;}
         */
        String descriptorName() {
            return DescriptorUtils.typeToString(type);
        }

    }

}
