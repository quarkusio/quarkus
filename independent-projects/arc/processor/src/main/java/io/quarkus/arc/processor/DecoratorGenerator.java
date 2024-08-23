package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class DecoratorGenerator extends BeanGenerator {

    protected static final String FIELD_NAME_DECORATED_TYPES = "decoratedTypes";
    protected static final String FIELD_NAME_DELEGATE_TYPE = "delegateType";
    static final String ABSTRACT_IMPL_SUFFIX = "_Impl";

    public DecoratorGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(annotationLiterals, applicationClassPredicate, privateMembers, generateSources, reflectionRegistration,
                existingClasses, beanToGeneratedName, injectionPointAnnotationsPredicate, Collections.emptyList());
    }

    /**
     * Precompute the generated name for the given decorator so that the {@link ComponentsProviderGenerator} can be executed
     * before all decorators metadata are generated.
     *
     * @param decorator
     */
    void precomputeGeneratedName(DecoratorInfo decorator) {
        ProviderType providerType = new ProviderType(decorator.getProviderType());
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();
        String baseName = createBaseName(decoratorClass);
        String targetPackage = DotNames.packageName(providerType.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        beanToGeneratedName.put(decorator, generatedName);
        beanToGeneratedBaseName.put(decorator, baseName);
    }

    /**
     *
     * @param decorator bean
     * @return a collection of resources
     */
    Collection<Resource> generate(DecoratorInfo decorator) {
        String baseName = beanToGeneratedBaseName.get(decorator);
        String generatedName = beanToGeneratedName.get(decorator);
        ProviderType providerType = new ProviderType(decorator.getProviderType());
        String targetPackage = DotNames.packageName(providerType.name());
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(decorator.getBeanClass())
                || decorator.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.DECORATOR_BEAN : null, generateSources);

        // MyDecorator_Bean implements InjectableDecorator<T>
        ClassCreator decoratorCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(InjectableDecorator.class, Supplier.class)
                .build();

        // Generate the implementation class for an abstract decorator
        if (decorator.isAbstract()) {
            String generatedImplName = generateDecoratorImplementation(baseName, targetPackage, decorator, decoratorClass,
                    classOutput);
            providerType = new ProviderType(org.jboss.jandex.Type.create(DotName.createSimple(generatedImplName), Kind.CLASS));
        }

        // Fields
        FieldCreator beanTypes = decoratorCreator.getFieldCreator(FIELD_NAME_BEAN_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator decoratedTypes = decoratorCreator.getFieldCreator(FIELD_NAME_DECORATED_TYPES, Set.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        InjectionPointInfo delegateInjectionPoint = decorator.getDelegateInjectionPoint();
        FieldCreator delegateType = decoratorCreator.getFieldCreator(FIELD_NAME_DELEGATE_TYPE, Type.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator delegateQualifiers = null;
        if (!delegateInjectionPoint.hasDefaultedQualifier()) {
            delegateQualifiers = decoratorCreator.getFieldCreator(FIELD_NAME_QUALIFIERS, Set.class)
                    .setModifiers(ACC_PRIVATE | ACC_FINAL);
        }

        Map<InjectionPointInfo, String> injectionPointToProviderField = new HashMap<>();
        initMaps(decorator, injectionPointToProviderField, Collections.emptyMap(), Collections.emptyMap());
        createProviderFields(decoratorCreator, decorator, injectionPointToProviderField, Collections.emptyMap(),
                Collections.emptyMap());
        createConstructor(classOutput, decoratorCreator, decorator, injectionPointToProviderField,
                delegateType, delegateQualifiers, decoratedTypes, reflectionRegistration);

        implementGetIdentifier(decorator, decoratorCreator);
        implementSupplierGet(decoratorCreator);
        implementCreate(classOutput, decoratorCreator, decorator, providerType, baseName,
                injectionPointToProviderField, Collections.emptyMap(), Collections.emptyMap(),
                targetPackage, isApplicationClass);
        if (decorator.hasDestroyLogic()) {
            implementDestroy(decorator, decoratorCreator, providerType, Collections.emptyMap(), isApplicationClass, baseName,
                    targetPackage);
        }
        implementGet(decorator, decoratorCreator, providerType, baseName);
        implementGetTypes(decoratorCreator, beanTypes.getFieldDescriptor());
        implementGetBeanClass(decorator, decoratorCreator);
        // Decorators are always @Dependent and have always default qualifiers

        // InjectableDecorator methods
        implementGetDecoratedTypes(decoratorCreator, decoratedTypes.getFieldDescriptor());
        implementGetDelegateType(decoratorCreator, delegateType.getFieldDescriptor());
        implementGetDelegateQualifiers(decoratorCreator, delegateQualifiers);
        implementGetPriority(decoratorCreator, decorator);

        decoratorCreator.close();

        return classOutput.getResources();

    }

    static String createBaseName(ClassInfo decoratorClass) {
        String baseName;
        if (decoratorClass.enclosingClass() != null) {
            baseName = DotNames.simpleName(decoratorClass.enclosingClass()) + "_" + DotNames.simpleName(decoratorClass);
        } else {
            baseName = DotNames.simpleName(decoratorClass);
        }
        return baseName;
    }

    static boolean isAbstractDecoratorImpl(BeanInfo bean, String providerTypeName) {
        return bean.isDecorator() && ((DecoratorInfo) bean).isAbstract() && providerTypeName.endsWith(ABSTRACT_IMPL_SUFFIX);
    }

    private String generateDecoratorImplementation(String baseName, String targetPackage, DecoratorInfo decorator,
            ClassInfo decoratorClass, ClassOutput classOutput) {
        // MyDecorator_Impl
        String generatedImplName = generatedNameFromTarget(targetPackage, baseName, ABSTRACT_IMPL_SUFFIX);
        ClassCreator decoratorImplCreator = ClassCreator.builder().classOutput(classOutput).className(generatedImplName)
                .superClass(decoratorClass.name().toString())
                .build();
        IndexView index = decorator.getDeployment().getBeanArchiveIndex();

        FieldCreator delegateField = decoratorImplCreator.getFieldCreator("impl$delegate",
                Object.class.getName());

        // Constructor
        MethodInfo decoratorConstructor = decoratorClass.firstMethod(Methods.INIT);
        List<String> decoratorConstructorParams = new ArrayList<>();
        for (org.jboss.jandex.Type parameterType : decoratorConstructor.parameterTypes()) {
            decoratorConstructorParams.add(parameterType.name().toString());
        }
        decoratorConstructorParams.add(CreationalContext.class.getName());
        MethodCreator constructor = decoratorImplCreator.getMethodCreator(Methods.INIT, "V",
                decoratorConstructorParams.toArray(new Object[0]));
        ResultHandle[] constructorArgs = new ResultHandle[decoratorConstructor.parametersCount()];
        for (int i = 0; i < decoratorConstructor.parametersCount(); i++) {
            constructorArgs[i] = constructor.getMethodParam(i);
        }
        // Invoke super()
        constructor.invokeSpecialMethod(decoratorConstructor, constructor.getThis(), constructorArgs);
        // Set the delegate field
        constructor.writeInstanceField(delegateField.getFieldDescriptor(), constructor.getThis(),
                constructor.invokeStaticMethod(MethodDescriptors.DECORATOR_DELEGATE_PROVIDER_GET,
                        constructor.getMethodParam(decoratorConstructor.parametersCount())));
        constructor.returnValue(null);

        // Find non-decorated methods from all decorated types
        Set<MethodDescriptor> abstractMethods = new HashSet<>();
        Map<MethodDescriptor, MethodDescriptor> bridgeMethods = new HashMap<>();
        for (org.jboss.jandex.Type decoratedType : decorator.getDecoratedTypes()) {

            ClassInfo decoratedTypeClass = index
                    .getClassByName(decoratedType.name());
            if (decoratedTypeClass == null) {
                throw new IllegalStateException("Decorated type not found in the bean archive index: " + decoratedType);
            }

            // A decorated type can declare type parameters
            // For example Converter<String> should result in a T -> String mapping
            List<TypeVariable> typeParameters = decoratedTypeClass.typeParameters();
            Map<String, org.jboss.jandex.Type> resolvedTypeParameters = Types.resolveDecoratedTypeParams(decoratedTypeClass,
                    decorator);

            for (MethodInfo method : decoratedTypeClass.methods()) {
                if (Methods.skipForDelegateSubclass(method)) {
                    continue;
                }
                MethodDescriptor methodDescriptor = MethodDescriptor.of(method);

                // Create a resolved descriptor variant if a param contains a type variable
                // E.g. ping(T) -> ping(String)
                MethodDescriptor resolvedMethodDescriptor;
                if (!typeParameters.isEmpty() && (Methods.containsTypeVariableParameter(method)
                        || Types.containsTypeVariable(method.returnType()))) {
                    List<org.jboss.jandex.Type> paramTypes = Types.getResolvedParameters(decoratedTypeClass,
                            resolvedTypeParameters, method,
                            index);
                    org.jboss.jandex.Type returnType = Types.resolveTypeParam(method.returnType(), resolvedTypeParameters,
                            index);
                    String[] paramTypesArray = new String[paramTypes.size()];
                    for (int i = 0; i < paramTypesArray.length; i++) {
                        paramTypesArray[i] = DescriptorUtils.typeToString(paramTypes.get(i));
                    }
                    resolvedMethodDescriptor = MethodDescriptor.ofMethod(method.declaringClass().toString(),
                            method.name(), DescriptorUtils.typeToString(returnType), paramTypesArray);
                } else {
                    resolvedMethodDescriptor = null;
                }

                MethodDescriptor abstractDescriptor = methodDescriptor;
                for (MethodInfo decoratorMethod : decoratorClass.methods()) {
                    MethodDescriptor descriptor = MethodDescriptor.of(decoratorMethod);
                    if (Methods.descriptorMatches(descriptor, methodDescriptor)) {
                        abstractDescriptor = null;
                        break;
                    }
                }

                if (abstractDescriptor != null) {
                    abstractMethods.add(methodDescriptor);
                    // Bridge method is needed
                    if (resolvedMethodDescriptor != null) {
                        bridgeMethods.put(resolvedMethodDescriptor, abstractDescriptor);
                    }
                }
            }
        }

        for (MethodDescriptor abstractMethod : abstractMethods) {
            MethodCreator delegate = decoratorImplCreator.getMethodCreator(abstractMethod)
                    .setModifiers(ACC_PUBLIC);
            // Invoke the method upon the delegate injection point
            ResultHandle delegateHandle = delegate.readInstanceField(delegateField.getFieldDescriptor(),
                    delegate.getThis());
            ResultHandle[] args = new ResultHandle[abstractMethod.getParameterTypes().length];
            for (int i = 0; i < args.length; i++) {
                args[i] = delegate.getMethodParam(i);
            }
            delegate.returnValue(delegate.invokeInterfaceMethod(abstractMethod, delegateHandle, args));
        }

        for (Entry<MethodDescriptor, MethodDescriptor> entry : bridgeMethods.entrySet()) {
            MethodCreator delegate = decoratorImplCreator.getMethodCreator(entry.getKey())
                    .setModifiers(ACC_PUBLIC);
            ResultHandle[] args = new ResultHandle[entry.getKey().getParameterTypes().length];
            for (int i = 0; i < args.length; i++) {
                args[i] = delegate.getMethodParam(i);
            }
            delegate.returnValue(
                    delegate.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(decoratorImplCreator.getClassName(), entry.getValue().getName(),
                                    entry.getValue().getReturnType(), entry.getValue().getParameterTypes()),
                            delegate.getThis(), args));
        }

        decoratorImplCreator.close();
        return generatedImplName;
    }

    protected void createConstructor(ClassOutput classOutput, ClassCreator creator, DecoratorInfo decorator,
            Map<InjectionPointInfo, String> injectionPointToProviderField,
            FieldCreator delegateType,
            FieldCreator delegateQualifiers,
            FieldCreator decoratedTypes,
            ReflectionRegistration reflectionRegistration) {

        MethodCreator constructor = initConstructor(classOutput, creator, decorator, injectionPointToProviderField,
                Collections.emptyMap(), Collections.emptyMap(), annotationLiterals, reflectionRegistration);

        if (delegateQualifiers != null) {
            // delegateQualifiers = new HashSet<>()
            ResultHandle delegateQualifiersHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
            for (AnnotationInstance delegateQualifier : decorator.getDelegateQualifiers()) {
                // Create annotation literal first
                ClassInfo delegateQualifierClass = decorator.getDeployment().getQualifier(delegateQualifier.name());
                constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, delegateQualifiersHandle,
                        annotationLiterals.create(constructor, delegateQualifierClass, delegateQualifier));
            }
            constructor.writeInstanceField(delegateQualifiers.getFieldDescriptor(), constructor.getThis(),
                    delegateQualifiersHandle);
        }

        // decoratedTypes = new HashSet<>()
        ResultHandle decoratedTypesHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashSet.class));
        // Get the TCCL - we will use it later
        ResultHandle currentThread = constructor
                .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
        ResultHandle tccl = constructor.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);
        for (org.jboss.jandex.Type decoratedType : decorator.getDecoratedTypes()) {
            ResultHandle typeHandle;
            try {
                typeHandle = Types.getTypeHandle(constructor, decoratedType, tccl);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unable to construct the type handle for " + decorator + ": " + e.getMessage());
            }
            constructor.invokeInterfaceMethod(MethodDescriptors.SET_ADD, decoratedTypesHandle, typeHandle);
        }
        constructor.writeInstanceField(decoratedTypes.getFieldDescriptor(), constructor.getThis(), decoratedTypesHandle);

        // delegate type
        ResultHandle delegateTypeHandle;
        try {
            delegateTypeHandle = Types.getTypeHandle(constructor, decorator.getDelegateType(), tccl);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unable to construct the type handle for " + decorator + ": " + e.getMessage());
        }
        constructor.writeInstanceField(delegateType.getFieldDescriptor(), constructor.getThis(), delegateTypeHandle);

        constructor.returnValue(null);
    }

    /**
     *
     * @see InjectableDecorator#getDecoratedTypes()
     */
    protected void implementGetDecoratedTypes(ClassCreator creator, FieldDescriptor decoratedTypes) {
        MethodCreator getDecoratedTypes = creator.getMethodCreator("getDecoratedTypes", Set.class).setModifiers(ACC_PUBLIC);
        getDecoratedTypes.returnValue(getDecoratedTypes.readInstanceField(decoratedTypes, getDecoratedTypes.getThis()));
    }

    /**
     *
     * @see InjectableDecorator#getDelegateType()
     */
    protected void implementGetDelegateType(ClassCreator creator, FieldDescriptor delegateType) {
        MethodCreator getDelegateType = creator.getMethodCreator("getDelegateType", Type.class).setModifiers(ACC_PUBLIC);
        getDelegateType.returnValue(getDelegateType.readInstanceField(delegateType, getDelegateType.getThis()));
    }

    /**
     *
     * @param creator
     * @param qualifiersField
     * @see InjectableDecorator#getDelegateQualifiers()
     */
    protected void implementGetDelegateQualifiers(ClassCreator creator, FieldCreator qualifiersField) {
        if (qualifiersField != null) {
            MethodCreator getDelegateQualifiers = creator.getMethodCreator("getDelegateQualifiers", Set.class)
                    .setModifiers(ACC_PUBLIC);
            getDelegateQualifiers
                    .returnValue(getDelegateQualifiers.readInstanceField(qualifiersField.getFieldDescriptor(),
                            getDelegateQualifiers.getThis()));
        }
    }

    /**
     *
     * @see InjectableDecorator#getPriority()
     */
    protected void implementGetPriority(ClassCreator creator, DecoratorInfo decorator) {
        MethodCreator getPriority = creator.getMethodCreator("getPriority", int.class).setModifiers(ACC_PUBLIC);
        getPriority.returnValue(getPriority.load(decorator.getPriority()));
    }

}
